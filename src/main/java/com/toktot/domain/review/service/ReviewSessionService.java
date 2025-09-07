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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewSessionService {

    private final RedisTemplate<String, Object> redisTemplate;
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
        Object sessionObject = redisTemplate.opsForValue().get(sessionKey);

        if (sessionObject == null) {
            return Optional.empty();
        }

        ReviewSessionDTO session = objectMapper.convertValue(sessionObject, ReviewSessionDTO.class);
        if (session == null) {
            redisTemplate.delete(sessionKey);
            return Optional.empty();
        }

        extendSessionTtl(sessionKey);
        return Optional.of(session);
    }

    public void saveSession(ReviewSessionDTO session) {
        String sessionKey = buildSessionKey(session.getUserId(), session.getRestaurantId());
        Duration ttl = Duration.ofHours(sessionTtlHours);

        redisTemplate.opsForValue().set(sessionKey, session, ttl);
    }

    public boolean tryAddImageToSession(Long userId, Long restaurantId, ReviewImageDTO imageDTO) {
        String sessionKey = buildSessionKey(userId, restaurantId);
        String imageJson = serializeToJson(imageDTO);
        String timestamp = DateTimeUtil.nowWithoutNanos().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

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

        if (!success) {
            log.warn("Failed to add image to session - max images reached: userId={}, restaurantId={}, imageId={}",
                    userId, restaurantId, imageDTO.getImageId());
        }

        return success;
    }

    public void removeImageFromSession(Long userId, Long restaurantId, String imageId) {
        ReviewSessionDTO session = getSession(userId, restaurantId)
                .orElseThrow(() -> new ToktotException(ErrorCode.RESOURCE_NOT_FOUND, "세션을 찾을 수 없습니다."));

        if (!session.hasImage(imageId)) {
            throw new ToktotException(ErrorCode.RESOURCE_NOT_FOUND, "이미지를 찾을 수 없습니다.");
        }

        session.removeImage(imageId);
        reorderImages(session);
        saveSession(session);
    }

    public void deleteSession(Long userId, Long restaurantId) {
        String sessionKey = buildSessionKey(userId, restaurantId);
        redisTemplate.delete(sessionKey);
    }

    private void reorderImages(ReviewSessionDTO session) {
        if (session.getImages() != null) {
            for (int i = 0; i < session.getImages().size(); i++) {
                session.getImages().get(i).setOrder(i + 1);
            }
        }
    }

    private void extendSessionTtl(String sessionKey) {
        Duration ttl = Duration.ofHours(sessionTtlHours);
        redisTemplate.expire(sessionKey, ttl);
    }

    private String buildSessionKey(Long userId, Long restaurantId) {
        return String.format("%s:%d:%d", SESSION_KEY_PREFIX, userId, restaurantId);
    }

    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("JSON serialization error", e);
            throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR, "객체를 JSON으로 변환하는 데 실패했습니다.");
        }
    }
}
