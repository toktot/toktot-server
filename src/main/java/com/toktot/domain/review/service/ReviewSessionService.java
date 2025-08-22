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
                externalKakaoId = tonumber(ARGV[3]),
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

    public Optional<ReviewSessionDTO> getSession(Long userId, String externalKakaoId) {
        String sessionKey = buildSessionKey(userId, externalKakaoId);
        String sessionJson = redisTemplate.opsForValue().get(sessionKey);

        if (sessionJson == null) {
            return Optional.empty();
        }

        ReviewSessionDTO session = deserializeSession(sessionJson);
        if (session == null) {
            redisTemplate.delete(sessionKey);
            return Optional.empty();
        }

        extendSessionTtl(sessionKey);
        return Optional.of(session);
    }

    public void saveSession(ReviewSessionDTO session) {
        String sessionKey = buildSessionKey(session.getUserId(), session.getExternalKakaoId());
        String sessionJson = serializeSession(session);
        Duration ttl = Duration.ofHours(sessionTtlHours);

        redisTemplate.opsForValue().set(sessionKey, sessionJson, ttl);
    }

    public boolean tryAddImageToSession(Long userId, String externalKakaoId, ReviewImageDTO imageDTO) {
        String sessionKey = buildSessionKey(userId, externalKakaoId);
        String imageJson = serializeImageDTO(imageDTO);
        String timestamp = DateTimeUtil.nowWithoutNanos().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Long result = redisTemplate.execute(
                RedisScript.of(LUA_SCRIPT_ADD_IMAGE, Long.class),
                List.of(sessionKey),
                imageJson,
                userId.toString(),
                externalKakaoId.toString(),
                timestamp,
                String.valueOf(MAX_IMAGES),
                String.valueOf(sessionTtlHours * 3600)
        );

        boolean success = result != null && result == 1;

        if (!success) {
            log.warn("Failed to add image to session - max images reached: userId={}, externalKakaoId={}, imageId={}",
                    userId, externalKakaoId, imageDTO.getImageId());
        }

        return success;
    }

    public void removeImageFromSession(Long userId, String externalKakaoId, String imageId) {
        Optional<ReviewSessionDTO> sessionOpt = getSession(userId, externalKakaoId);

        if (sessionOpt.isEmpty()) {
            throw new ToktotException(ErrorCode.RESOURCE_NOT_FOUND, "세션을 찾을 수 없습니다.");
        }

        ReviewSessionDTO session = sessionOpt.get();

        if (!session.hasImage(imageId)) {
            throw new ToktotException(ErrorCode.RESOURCE_NOT_FOUND, "이미지를 찾을 수 없습니다.");
        }

        session.removeImage(imageId);
        reorderImages(session);
        saveSession(session);
    }

    public void deleteSession(Long userId, String externalKakaoId) {
        String sessionKey = buildSessionKey(userId, externalKakaoId);
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

    private String buildSessionKey(Long userId, String externalKakaoId) {
        return String.format("%s:%d:%s", SESSION_KEY_PREFIX, userId, externalKakaoId);
    }

    private String serializeSession(ReviewSessionDTO session) {
        try {
            return objectMapper.writeValueAsString(session);
        } catch (JsonProcessingException e) {
            throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    private ReviewSessionDTO deserializeSession(String sessionJson) {
        try {
            return objectMapper.readValue(sessionJson, ReviewSessionDTO.class);
        } catch (JsonProcessingException e) {
            throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    private String serializeImageDTO(ReviewImageDTO imageDTO) {
        try {
            return objectMapper.writeValueAsString(imageDTO);
        } catch (JsonProcessingException e) {
            throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }
}
