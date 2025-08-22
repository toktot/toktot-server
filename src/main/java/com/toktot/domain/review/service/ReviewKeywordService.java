package com.toktot.domain.review.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
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

    public void saveKeywordsInReview(Review review, List<KeywordType> keywordTypes) {
        List<KeywordType> uniqueKeywordTypes = keywordTypes.stream()
                .distinct()
                .toList();

        if (uniqueKeywordTypes.size() != keywordTypes.size()) {
            throw new ToktotException(ErrorCode.INVALID_INPUT, "중복된 키워드가 존재합니다.");
        }

        for (KeywordType keywordType : uniqueKeywordTypes) {
            ReviewKeyword reviewKeyword = ReviewKeyword.create(keywordType);
            review.addKeyword(reviewKeyword);
        }
    }

}
