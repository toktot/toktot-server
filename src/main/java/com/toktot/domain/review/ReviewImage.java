package com.toktot.domain.review;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "s3_key", length = 500, nullable = false)
    private String s3Key;

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
    private List<Tooltip> tooltips = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ReviewImage create(String imageId, String s3Key, String imageUrl,
                                     Long fileSize, Integer imageOrder, Boolean isMain) {
        return ReviewImage.builder()
                .imageId(imageId)
                .s3Key(s3Key)
                .imageUrl(imageUrl)
                .fileSize(fileSize)
                .imageOrder(imageOrder)
                .tooltips(new ArrayList<>())
                .isMain(isMain)
                .build();
    }

    public void assignReview(Review review) {
        this.review = review;
    }

    public void addTooltip(Tooltip tooltip) {
        this.tooltips.add(tooltip);
        tooltip.assignReviewImage(this);
    }

}
