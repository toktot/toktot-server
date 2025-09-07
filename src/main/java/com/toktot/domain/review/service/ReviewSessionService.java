package com.toktot.domain.review.service;

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
import org.springframework.stereotype.Service;

import java.time.Duration;
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

    public Optional<ReviewSessionDTO> getSession(Long userId, Long restaurantId) {
        try {
            String sessionKey = buildSessionKey(userId, restaurantId);
            Object sessionObject = redisTemplate.opsForValue().get(sessionKey);

            if (sessionObject == null) {
                log.debug("Session not found: {}", sessionKey);
                return Optional.empty();
            }

            ReviewSessionDTO session;
            if (sessionObject instanceof ReviewSessionDTO) {
                session = (ReviewSessionDTO) sessionObject;
            } else {
                session = objectMapper.convertValue(sessionObject, ReviewSessionDTO.class);
            }

            if (session == null) {
                log.warn("Failed to deserialize session: {}", sessionKey);
                redisTemplate.delete(sessionKey);
                return Optional.empty();
            }

            extendSessionTtl(sessionKey);
            log.debug("Session retrieved successfully: userId={}, restaurantId={}, imageCount={}",
                    userId, restaurantId, session.getImageCount());
            return Optional.of(session);

        } catch (Exception e) {
            log.error("Error getting session: userId={}, restaurantId={}", userId, restaurantId, e);
            return Optional.empty();
        }
    }

    public void saveSession(ReviewSessionDTO session) {
        try {
            String sessionKey = buildSessionKey(session.getUserId(), session.getRestaurantId());
            Duration ttl = Duration.ofHours(sessionTtlHours);

            redisTemplate.opsForValue().set(sessionKey, session, ttl);

            log.debug("Session saved: userId={}, restaurantId={}, imageCount={}",
                    session.getUserId(), session.getRestaurantId(), session.getImageCount());

        } catch (Exception e) {
            log.error("Error saving session: userId={}, restaurantId={}",
                    session.getUserId(), session.getRestaurantId(), e);
            throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR, "세션 저장에 실패했습니다.");
        }
    }

    public boolean tryAddImageToSession(Long userId, Long restaurantId, ReviewImageDTO imageDTO) {
        try {
            ReviewSessionDTO session = getSession(userId, restaurantId)
                    .orElse(ReviewSessionDTO.create(userId, restaurantId));

            if (session.getImageCount() >= MAX_IMAGES) {
                log.warn("Failed to add image to session - max images reached: userId={}, restaurantId={}, imageId={}",
                        userId, restaurantId, imageDTO.getImageId());
                return false;
            }

            imageDTO.setOrder(session.getImageCount() + 1);
            session.getImages().add(imageDTO);
            session.setLastModified(DateTimeUtil.nowWithoutNanos());

            saveSession(session);

            log.debug("Image added to session: userId={}, restaurantId={}, imageId={}",
                    userId, restaurantId, imageDTO.getImageId());
            return true;

        } catch (Exception e) {
            log.error("Error adding image to session: userId={}, restaurantId={}, imageId={}",
                    userId, restaurantId, imageDTO.getImageId(), e);
            return false;
        }
    }

    public void removeImageFromSession(Long userId, Long restaurantId, String imageId) {
        try {
            ReviewSessionDTO session = getSession(userId, restaurantId)
                    .orElseThrow(() -> new ToktotException(ErrorCode.RESOURCE_NOT_FOUND, "세션을 찾을 수 없습니다."));

            if (!session.hasImage(imageId)) {
                throw new ToktotException(ErrorCode.RESOURCE_NOT_FOUND, "이미지를 찾을 수 없습니다.");
            }

            session.removeImage(imageId);
            reorderImages(session);
            saveSession(session);

            log.debug("Image removed from session: userId={}, restaurantId={}, imageId={}",
                    userId, restaurantId, imageId);

        } catch (ToktotException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error removing image from session: userId={}, restaurantId={}, imageId={}",
                    userId, restaurantId, imageId, e);
            throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR, "이미지 삭제에 실패했습니다.");
        }
    }

    public void deleteSession(Long userId, Long restaurantId) {
        try {
            String sessionKey = buildSessionKey(userId, restaurantId);
            redisTemplate.delete(sessionKey);
            log.debug("Session deleted: userId={}, restaurantId={}", userId, restaurantId);

        } catch (Exception e) {
            log.error("Error deleting session: userId={}, restaurantId={}", userId, restaurantId, e);
        }
    }

    private void reorderImages(ReviewSessionDTO session) {
        if (session.getImages() != null) {
            for (int i = 0; i < session.getImages().size(); i++) {
                session.getImages().get(i).setOrder(i + 1);
            }
        }
    }

    private void extendSessionTtl(String sessionKey) {
        try {
            Duration ttl = Duration.ofHours(sessionTtlHours);
            redisTemplate.expire(sessionKey, ttl);
        } catch (Exception e) {
            log.warn("Failed to extend session TTL: {}", sessionKey, e);
        }
    }

    private String buildSessionKey(Long userId, Long restaurantId) {
        return String.format("%s:%d:%d", SESSION_KEY_PREFIX, userId, restaurantId);
    }
}
