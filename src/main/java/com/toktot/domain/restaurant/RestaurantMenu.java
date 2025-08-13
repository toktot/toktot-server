package com.toktot.domain.restaurant;

import com.toktot.domain.localfood.LocalFoodType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Builder
@Entity
@Table(name = "restaurant_menus")
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

    @Column(name = "menu_name", nullable = false, length = 100)
    private String menuName;

    @Column(name = "normalized_name", nullable = false, length = 100)
    private String normalizedName;

    @Column
    private Integer price;

    @Column(name = "serving_size")
    private Integer servingSize = 1;

    @Column(name = "price_per_serving")
    private Integer pricePerServing;

    @Column(length = 50)
    private String category;

    @Column(name = "is_local_food", nullable = false)
    private Boolean isLocalFood = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "local_food_type", length = 50)
    private LocalFoodType localFoodType;

    @Column(name = "is_representative", nullable = false)
    private Boolean isRepresentative = false;

    @Column(name = "seasonal_available", nullable = false)
    private Boolean seasonalAvailable = true;

    @Column(name = "input_type", nullable = false, length = 20)
    private String inputType = "ADMIN";

    @Column(name = "input_by")
    private Long inputBy;

    @Column(name = "menu_image_url", length = 500)
    private String menuImageUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
