package com.toktot.domain.review;

import com.toktot.domain.review.type.TooltipType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tooltips")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Tooltip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_image_id", nullable = false)
    private ReviewImage reviewImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "tooltip_type", length = 20, nullable = false)
    private TooltipType tooltipType;

    @Column(name = "x_position", precision = 5, scale = 2, nullable = false)
    private BigDecimal xPosition;

    @Column(name = "y_position", precision = 5, scale = 2, nullable = false)
    private BigDecimal yPosition;

    @Column(name = "rating", precision = 2, scale = 1, nullable = false)
    private BigDecimal rating;

    @Column(name = "menu_name", length = 100)
    private String menuName;

    @Column(name = "total_price")
    private Integer totalPrice;

    @Column(name = "serving_size")
    private Integer servingSize;

    @Column(name = "detailed_review", length = 100)
    private String detailedReview;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Tooltip createFoodTooltip(BigDecimal xPosition, BigDecimal yPosition,
                                            String menuName, Integer totalPrice, Integer servingSize,
                                            BigDecimal rating, String detailedReview) {
        return Tooltip.builder()
                .tooltipType(TooltipType.FOOD)
                .xPosition(xPosition)
                .yPosition(yPosition)
                .rating(rating)
                .menuName(menuName)
                .totalPrice(totalPrice)
                .servingSize(servingSize)
                .detailedReview(detailedReview)
                .build();
    }

    public static Tooltip createServiceTooltip(TooltipType type, BigDecimal xPosition,
                                               BigDecimal yPosition, BigDecimal rating) {
        return Tooltip.builder()
                .tooltipType(type)
                .xPosition(xPosition)
                .yPosition(yPosition)
                .rating(rating)
                .build();
    }

    public void assignReviewImage(ReviewImage reviewImage) {
        this.reviewImage = reviewImage;
    }

}
