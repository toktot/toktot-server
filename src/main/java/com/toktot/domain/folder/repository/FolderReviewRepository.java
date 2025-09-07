package com.toktot.domain.folder.repository;

import com.toktot.domain.folder.FolderReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FolderReviewRepository extends JpaRepository<FolderReview, Long> {

    @Query("SELECT fr.review.id FROM FolderReview fr GROUP BY fr.review.id ORDER BY COUNT(fr.review.id) DESC")
    List<Long> findPopularReviewIds(Pageable pageable);
}
