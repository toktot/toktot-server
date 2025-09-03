package com.toktot.domain.folder.repository;

import com.toktot.domain.folder.Folder;
import com.toktot.domain.user.User;
import com.toktot.domain.folder.dto.response.FolderResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    @Query("SELECT new com.toktot.domain.folder.dto.response.FolderResponse(" +
            "f.id, f.folderName, COUNT(fr.review.id), f.createdAt, f.isDefault) " +
            "FROM Folder f " +
            "LEFT JOIN f.folderReviews fr " +
            "WHERE f.user.id = :userId " +
            "GROUP BY f.id " +
            "ORDER BY f.isDefault DESC, f.createdAt ASC")
    List<FolderResponse> findFoldersWithReviewCountByUserId(@Param("userId") Long userId);

    boolean existsFolderByUserIdAndId(Long userId, Long id);

    boolean existsByUserAndIsDefaultTrue(User user);
}
