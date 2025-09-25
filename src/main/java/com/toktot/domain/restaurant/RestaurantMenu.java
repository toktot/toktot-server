package com.toktot.domain.restaurant;

import com.toktot.domain.localfood.LocalFoodType;
import com.toktot.domain.restaurant.type.MenuCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Builder
@Entity
@Table(name = "restaurant_menus", indexes = {
        @Index(name = "idx_restaurant_menu_active", columnList = "restaurant_id, is_active"),
        @Index(name = "idx_normalized_name", columnList = "normalized_name"),
        @Index(name = "idx_local_food_type", columnList = "local_food_type, is_active")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RestaurantMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false, length = 100)
    private String menuName;

    private Integer price;

    @Builder.Default
    private Integer servingSize = 0;

    private Integer pricePerServing;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private MenuCategory category;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isLocalFood = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private LocalFoodType localFoodType;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isMain = false;

    @Column(length = 500)
    private String menuImageUrl;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void calculatePricePerServing() {
        if (price == null || servingSize == null || servingSize <= 0) {
            this.pricePerServing = null;
            return;
        }

        this.pricePerServing = price / servingSize;
    }
}
