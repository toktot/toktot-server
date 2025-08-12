package com.toktot.domain.review;

import com.toktot.domain.report.ReviewReport;
import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "reviews")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewKeyword> keywords = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "report_count", nullable = false)
    private Integer reportCount = 0;

    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden = false;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewReport> reports = new ArrayList<>();

    public static Review create(User user, Restaurant restaurant) {
        return Review.builder()
                .user(user)
                .restaurant(restaurant)
                .images(new ArrayList<>())
                .keywords(new ArrayList<>())
                .build();
    }

    public void addImage(ReviewImage reviewImage) {
        this.images.add(reviewImage);
        reviewImage.assignReview(this);
    }

    public void addKeyword(ReviewKeyword reviewKeyword) {
        this.keywords.add(reviewKeyword);
        reviewKeyword.assignReview(this);
    }

    public boolean isWriter(Long userId) {
        if (userId == null) {
            return false;
        }

        return Objects.equals(this.user.getId(), userId);
    }

    public void increaseReportCount() {
        this.reportCount++;
    }
}
