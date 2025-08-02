package com.toktot.domain.review.service;

import com.toktot.domain.review.Review;
import com.toktot.domain.review.ReviewKeyword;
import com.toktot.domain.review.type.KeywordType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewKeywordService {

    public void attachKeywords(Review review, List<KeywordType> keywordTypes) {
        log.atInfo()
                .setMessage("Starting keyword attachment process")
                .addKeyValue("reviewId", review.getId())
                .addKeyValue("keywordCount", keywordTypes.size())
                .addKeyValue("keywordTypes", keywordTypes.toArray())
                .log();

        try {
            // 키워드 타입 중복 제거
            List<KeywordType> uniqueKeywordTypes = keywordTypes.stream()
                    .distinct()
                    .toList();

            if (uniqueKeywordTypes.size() != keywordTypes.size()) {
                log.atDebug()
                        .setMessage("Duplicate keywords removed")
                        .addKeyValue("originalCount", keywordTypes.size())
                        .addKeyValue("uniqueCount", uniqueKeywordTypes.size())
                        .log();
            }

            for (KeywordType keywordType : uniqueKeywordTypes) {
                ReviewKeyword reviewKeyword = ReviewKeyword.create(keywordType);
                review.addKeyword(reviewKeyword);
            }

            log.atInfo()
                    .setMessage("Keyword attachment completed successfully")
                    .addKeyValue("reviewId", review.getId())
                    .addKeyValue("attachedKeywords", uniqueKeywordTypes.size())
                    .log();

        } catch (Exception e) {
            log.atError()
                    .setMessage("Keyword attachment failed")
                    .addKeyValue("reviewId", review.getId())
                    .addKeyValue("keywordTypes", keywordTypes.toArray())
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
            throw new RuntimeException("키워드 연결에 실패했습니다.", e);
        }
    }

}
