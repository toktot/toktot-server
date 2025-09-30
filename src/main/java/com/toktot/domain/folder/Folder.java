package com.toktot.domain.folder;

import com.toktot.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "folders")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Folder {

    private static final String DEFAULT_NEW_FOLDER_NAME = "이름이 없는 폴더";
    private static final String DEFAULT_FOLDER_NAME = "기본 폴더";
    private static final int MAX_FOLDER_NAME_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "folder_name", nullable = false, length = MAX_FOLDER_NAME_LENGTH)
    private String folderName;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FolderReview> folderReviews = new ArrayList<>();

    public static Folder createDefaultFolder(User user) {
        return Folder.builder()
                .user(user)
                .folderName(DEFAULT_FOLDER_NAME)
                .isDefault(true)
                .build();
    }

    public static Folder createNewFolder(User user, String folderName) {
        if (folderName == null || folderName.trim().isBlank()) {
            folderName = DEFAULT_NEW_FOLDER_NAME;
        }

        return Folder.builder()
                .user(user)
                .folderName(folderName.trim())
                .isDefault(false)
                .build();
    }

    public void updateFolderName(String newFolderName) {
        if (newFolderName == null || newFolderName.trim().isBlank()) {
            this.folderName = DEFAULT_NEW_FOLDER_NAME;
        } else {
            this.folderName = newFolderName.trim();
        }
    }
}
