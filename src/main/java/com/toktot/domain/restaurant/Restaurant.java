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

    public Restaurant updateFromTourApi(Restaurant newData) {
        if (newData == null) {
            return this;
        }

        return Restaurant.builder()
                .id(this.id)
                .externalTourApiId(newData.getExternalTourApiId() != null ? newData.getExternalTourApiId() : this.externalTourApiId)
                .externalKakaoId(this.externalKakaoId)
                .name(newData.getName() != null ? newData.getName() : this.name)
                .category(newData.getCategory() != null ? newData.getCategory() : this.category)
                .address(newData.getAddress() != null ? newData.getAddress() : this.address)
                .roadAddress(this.roadAddress)
                .latitude(newData.getLatitude() != null ? newData.getLatitude() : this.latitude)
                .longitude(newData.getLongitude() != null ? newData.getLongitude() : this.longitude)
                .phone(newData.getPhone() != null ? newData.getPhone() : this.phone)
                .isGoodPriceStore(this.isGoodPriceStore)
                .dataSource(this.dataSource != DataSource.USER_CREATED ? DataSource.TOUR_API : this.dataSource)
                .isActive(this.isActive)
                .searchCount(this.searchCount)
                .lastSyncedAt(LocalDateTime.now())
                .createdAt(this.createdAt)
                .build();
    }

}
