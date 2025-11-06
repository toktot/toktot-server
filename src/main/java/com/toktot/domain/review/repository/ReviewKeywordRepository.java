package com.toktot.domain.review.repository;

import com.toktot.domain.review.ReviewKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewKeywordRepository extends JpaRepository<ReviewKeyword, Long> {

    @Query("SELECT rk.review.id, rk.keywordType FROM ReviewKeyword rk WHERE rk.review.id IN :reviewIds")
    List<Object[]> findKeywordsByReviewIds(@Param("reviewIds") List<Long> reviewIds);
}