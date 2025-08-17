package com.toktot.domain.restaurant;

import com.toktot.domain.restaurant.type.DataSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Entity
@Table(name = "restaurants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_tour_api_id", length = 50)
    private String externalTourApiId;

    @Column(name = "external_kakao_id", length = 100)
    private String externalKakaoId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 50)
    private String category;

    @Column(length = 200)
    private String address;

    @Column(name = "road_address", length = 200)
    private String roadAddress;

    @Column(precision = 10, scale = 8, nullable = false)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8, nullable = false)
    private BigDecimal longitude;

    @Column(length = 20)
    private String phone;

    @Column(name = "is_good_price_store", nullable = false)
    private Boolean isGoodPriceStore = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 20)
    private DataSource dataSource = DataSource.USER_CREATED;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "search_count", nullable = false)
    private Integer searchCount = 0;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updateFromTourApi(Restaurant newData) {
        if (newData == null) {
            return;
        }

        if (newData.getName() != null) {
            this.name = newData.getName();
        }

        if (newData.getAddress() != null) {
            this.address = newData.getAddress();
        }

        if (newData.getPhone() != null) {
            this.phone = newData.getPhone();
        }

        if (newData.getCategory() != null) {
            this.category = newData.getCategory();
        }

        if (newData.getLatitude() != null) {
            this.latitude = newData.getLatitude();
        }

        if (newData.getLongitude() != null) {
            this.longitude = newData.getLongitude();
        }

        this.lastSyncedAt = LocalDateTime.now();

        if (this.dataSource != DataSource.USER_CREATED) {
            this.dataSource = DataSource.TOUR_API;
        }
    }
}
