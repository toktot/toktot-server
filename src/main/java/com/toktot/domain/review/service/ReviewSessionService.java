package com.toktot.domain.review.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.common.util.DateTimeUtil;
import com.toktot.domain.review.dto.ReviewImageDTO;
import com.toktot.domain.review.dto.ReviewSessionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewSessionService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${toktot.review.session.ttl-hours:2}")
    private int sessionTtlHours;

    private static final String SESSION_KEY_PREFIX = "review_session";
    private static final int MAX_IMAGES = 5;

    private static final String LUA_SCRIPT_ADD_IMAGE = """
        local sessionData = redis.call('GET', KEYS[1])
        local session
        
        if sessionData == false then
            session = {
                userId = tonumber(ARGV[2]),
                restaurantId = tonumber(ARGV[3]),
                images = {},
                createdAt = ARGV[4],
                lastModified = ARGV[4]
            }
        else
            session = cjson.decode(sessionData)
        end
        
        local imageCount = #session.images
        if imageCount >= tonumber(ARGV[5]) then
            return 0
        end
        
        local newImage = cjson.decode(ARGV[1])
        newImage.order = imageCount + 1
        table.insert(session.images, newImage)
        session.lastModified = ARGV[4]
        
        redis.call('SET', KEYS[1], cjson.encode(session), 'EX', tonumber(ARGV[6]))
        return 1
        """;

    public Optional<ReviewSessionDTO> getSession(Long userId, Long restaurantId) {
        String sessionKey = buildSessionKey(userId, restaurantId);

        log.atDebug()
                .setMessage("Retrieving session")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("sessionKey", sessionKey)
                .log();

        String sessionJson = redisTemplate.opsForValue().get(sessionKey);

        if (sessionJson == null) {
            log.atDebug()
                    .setMessage("Session not found")
                    .addKeyValue("userId", userId)
                    .addKeyValue("restaurantId", restaurantId)
                    .log();
            return Optional.empty();
        }

        ReviewSessionDTO session = deserializeSession(sessionJson);

        if (session == null) {
            log.atInfo()
                    .setMessage("Removing incompatible session data")
                    .addKeyValue("userId", userId)
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("sessionKey", sessionKey)
                    .log();

            redisTemplate.delete(sessionKey);
            return Optional.empty();
        }

        extendSessionTtl(sessionKey);

        log.atDebug()
                .setMessage("Session found and TTL extended")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("imageCount", session.getImageCount())
                .addKeyValue("lastModified", session.getLastModified())
                .log();

        return Optional.of(session);
    }

    public void saveSession(ReviewSessionDTO session) {
        String sessionKey = buildSessionKey(session.getUserId(), session.getRestaurantId());
        String sessionJson = serializeSession(session);
        Duration ttl = Duration.ofHours(sessionTtlHours);

        log.atDebug()
                .setMessage("Saving session")
                .addKeyValue("userId", session.getUserId())
                .addKeyValue("restaurantId", session.getRestaurantId())
                .addKeyValue("sessionKey", sessionKey)
                .addKeyValue("imageCount", session.getImageCount())
                .addKeyValue("ttlSeconds", ttl.toSeconds())
                .log();

        redisTemplate.opsForValue().set(sessionKey, sessionJson, ttl);

        log.atDebug()
                .setMessage("Session saved successfully")
                .addKeyValue("userId", session.getUserId())
                .addKeyValue("restaurantId", session.getRestaurantId())
                .log();
    }

    public boolean tryAddImageToSession(Long userId, Long restaurantId, ReviewImageDTO imageDTO) {
        String sessionKey = buildSessionKey(userId, restaurantId);

        log.atInfo()
                .setMessage("Attempting to add image to session atomically")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("imageId", imageDTO.getImageId())
                .addKeyValue("sessionKey", sessionKey)
                .log();

        try {
            String imageJson = serializeImageDTO(imageDTO);
            String timestamp = DateTimeUtil.nowWithoutNanos().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            Long result = redisTemplate.execute(
                    RedisScript.of(LUA_SCRIPT_ADD_IMAGE, Long.class),
                    List.of(sessionKey),
                    imageJson,
                    userId.toString(),
                    restaurantId.toString(),
                    timestamp,
                    String.valueOf(MAX_IMAGES),
                    String.valueOf(sessionTtlHours * 3600)
            );

            boolean success = result != null && result == 1;

            if (success) {
                log.atInfo()
                        .setMessage("Image added to session successfully")
                        .addKeyValue("userId", userId)
                        .addKeyValue("restaurantId", restaurantId)
                        .addKeyValue("imageId", imageDTO.getImageId())
                        .addKeyValue("s3Key", imageDTO.getS3Key())
                        .addKeyValue("order", imageDTO.getOrder())
                        .log();
            } else {
                log.atWarn()
                        .setMessage("Failed to add image to session - maximum images reached")
                        .addKeyValue("userId", userId)
                        .addKeyValue("restaurantId", restaurantId)
                        .addKeyValue("imageId", imageDTO.getImageId())
                        .addKeyValue("maxImages", MAX_IMAGES)
                        .addKeyValue("luaResult", result)
                        .log();
            }

            return success;

        } catch (Exception e) {
            log.atError()
                    .setMessage("Error occurred while adding image to session")
                    .addKeyValue("userId", userId)
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("imageId", imageDTO.getImageId())
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();

            return false;
        }
    }

    public void removeImageFromSession(Long userId, Long restaurantId, String imageId) {
        log.atInfo()
                .setMessage("Removing image from session")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("imageId", imageId)
                .log();

        Optional<ReviewSessionDTO> sessionOpt = getSession(userId, restaurantId);

        if (sessionOpt.isEmpty()) {
            log.atWarn()
                    .setMessage("Cannot remove image - session not found")
                    .addKeyValue("userId", userId)
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("imageId", imageId)
                    .log();
            throw new ToktotException(ErrorCode.RESOURCE_NOT_FOUND, "세션을 찾을 수 없습니다.");
        }

        ReviewSessionDTO session = sessionOpt.get();

        if (!session.hasImage(imageId)) {
            log.atWarn()
                    .setMessage("Cannot remove image - image not found in session")
                    .addKeyValue("userId", userId)
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("imageId", imageId)
                    .addKeyValue("sessionImageCount", session.getImageCount())
                    .log();
            throw new ToktotException(ErrorCode.RESOURCE_NOT_FOUND, "이미지를 찾을 수 없습니다.");
        }

        int beforeCount = session.getImageCount();
        session.removeImage(imageId);
        reorderImages(session);
        saveSession(session);

        log.atInfo()
                .setMessage("Image removed from session successfully")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("removedImageId", imageId)
                .addKeyValue("beforeCount", beforeCount)
                .addKeyValue("afterCount", session.getImageCount())
                .log();
    }

    public void deleteSession(Long userId, Long restaurantId) {
        String sessionKey = buildSessionKey(userId, restaurantId);

        log.atInfo()
                .setMessage("Deleting session")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("sessionKey", sessionKey)
                .log();

        Boolean deleted = redisTemplate.delete(sessionKey);

        log.atInfo()
                .setMessage("Session deletion completed")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("wasDeleted", Boolean.TRUE.equals(deleted))
                .log();
    }

    private void reorderImages(ReviewSessionDTO session) {
        if (session.getImages() != null) {
            log.atDebug()
                    .setMessage("Reordering images")
                    .addKeyValue("userId", session.getUserId())
                    .addKeyValue("restaurantId", session.getRestaurantId())
                    .addKeyValue("imageCount", session.getImages().size())
                    .log();

            for (int i = 0; i < session.getImages().size(); i++) {
                session.getImages().get(i).setOrder(i + 1);
            }

            log.atDebug()
                    .setMessage("Image reordering completed")
                    .addKeyValue("userId", session.getUserId())
                    .addKeyValue("restaurantId", session.getRestaurantId())
                    .log();
        }
    }

    private void extendSessionTtl(String sessionKey) {
        Duration ttl = Duration.ofHours(sessionTtlHours);
        Boolean extended = redisTemplate.expire(sessionKey, ttl);

        log.atDebug()
                .setMessage("Session TTL extended")
                .addKeyValue("sessionKey", sessionKey)
                .addKeyValue("ttlHours", sessionTtlHours)
                .addKeyValue("extended", Boolean.TRUE.equals(extended))
                .log();
    }

    private String buildSessionKey(Long userId, Long restaurantId) {
        String sessionKey = String.format("%s:%d:%d", SESSION_KEY_PREFIX, userId, restaurantId);

        log.atTrace()
                .setMessage("Built session key")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("sessionKey", sessionKey)
                .log();

        return sessionKey;
    }

    private String serializeSession(ReviewSessionDTO session) {
        try {
            String json = objectMapper.writeValueAsString(session);

            log.atTrace()
                    .setMessage("Session serialized")
                    .addKeyValue("userId", session.getUserId())
                    .addKeyValue("restaurantId", session.getRestaurantId())
                    .addKeyValue("jsonLength", json.length())
                    .log();

            return json;

        } catch (JsonProcessingException e) {
            log.atError()
                    .setMessage("Session serialization failed")
                    .addKeyValue("userId", session.getUserId())
                    .addKeyValue("restaurantId", session.getRestaurantId())
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
            throw new ToktotException(ErrorCode.SERVER_ERROR, "세션 데이터 처리 오류");
        }
    }

    private ReviewSessionDTO deserializeSession(String sessionJson) {
        try {
            ReviewSessionDTO session = objectMapper.readValue(sessionJson, ReviewSessionDTO.class);

            log.atTrace()
                    .setMessage("Session deserialized")
                    .addKeyValue("userId", session.getUserId())
                    .addKeyValue("restaurantId", session.getRestaurantId())
                    .addKeyValue("imageCount", session.getImageCount())
                    .addKeyValue("jsonLength", sessionJson.length())
                    .log();

            return session;

        } catch (JsonProcessingException e) {
            log.atWarn()
                    .setMessage("Session deserialization failed - possibly old format, will recreate")
                    .addKeyValue("jsonLength", sessionJson.length())
                    .addKeyValue("jsonPreview", sessionJson.substring(0, Math.min(100, sessionJson.length())))
                    .addKeyValue("error", e.getMessage())
                    .log();

            return null;
        }
    }

    private String serializeImageDTO(ReviewImageDTO imageDTO) {
        try {
            String json = objectMapper.writeValueAsString(imageDTO);

            log.atTrace()
                    .setMessage("Image DTO serialized")
                    .addKeyValue("imageId", imageDTO.getImageId())
                    .addKeyValue("jsonLength", json.length())
                    .log();

            return json;

        } catch (JsonProcessingException e) {
            log.atError()
                    .setMessage("Image DTO serialization failed")
                    .addKeyValue("imageId", imageDTO.getImageId())
                    .addKeyValue("s3Key", imageDTO.getS3Key())
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
            throw new ToktotException(ErrorCode.SERVER_ERROR, "이미지 데이터 처리 오류");
        }
    }
}
