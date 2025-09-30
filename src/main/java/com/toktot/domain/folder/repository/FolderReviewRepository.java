package com.toktot.domain.folder.repository;

import com.toktot.domain.folder.FolderReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FolderReviewRepository extends JpaRepository<FolderReview, Long> {

    @Query("SELECT fr.review.id FROM FolderReview fr GROUP BY fr.review.id ORDER BY COUNT(fr.review.id) DESC")
    List<Long> findPopularReviewIds(Pageable pageable);

    @Query("""
    SELECT fr.review.id 
    FROM FolderReview fr 
    WHERE fr.review.id IN :reviewIds 
    AND fr.folder.user.id = :userId
    """)
    List<Long> findBookmarkedReviewIds(@Param("reviewIds") List<Long> reviewIds,
                                       @Param("userId") Long userId);

    boolean existsByFolderIdAndReviewId(Long folderId, Long reviewId);

    Optional<FolderReview> findByFolderIdAndReviewId(Long folderId, Long reviewId);
}
