package com.toktot.domain.review;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "review_images")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "image_id", length = 100, nullable = false)
    private String imageId;

    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "image_order", nullable = false)
    private Integer imageOrder;

    @Builder.Default
    @Column(name = "is_main", nullable = false)
    private Boolean isMain = false;

    @OneToMany(mappedBy = "reviewImage", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @BatchSize(size = 20)
    private Set<Tooltip> tooltips = new HashSet<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ReviewImage create(String imageId, Long fileSize, Integer imageOrder, Boolean isMain) {
        return ReviewImage.builder()
                .imageId(imageId)
                .fileSize(fileSize)
                .imageOrder(imageOrder)
                .tooltips(new HashSet<>())
                .isMain(isMain)
                .build();
    }

    public void setImageUrl(Long restaurantId, Long reviewId, String extension) {
        this.imageUrl = "https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/reviews/" + restaurantId.toString() + "/" + reviewId.toString() + "/" + this.imageId + extension;
    }

    public void assignReview(Review review) {
        this.review = review;
    }

    public void addTooltip(Tooltip tooltip) {
        this.tooltips.add(tooltip);
        tooltip.assignReviewImage(this);
    }

}
