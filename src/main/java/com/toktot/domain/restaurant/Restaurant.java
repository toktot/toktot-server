package com.toktot.domain.restaurant;

import com.toktot.domain.restaurant.type.DataSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

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

    @Builder.Default
    @Column(name = "is_good_price_store", nullable = false)
    private Boolean isGoodPriceStore = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 20)
    private DataSource dataSource = DataSource.USER_CREATED;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "search_count", nullable = false)
    private Integer searchCount = 0;

    @UpdateTimestamp
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "image", length = 500)
    private String image;

    @Builder.Default
    @Column(name = "is_local_store", nullable = false)
    private Boolean isLocalStore = false;

    @Column(length = 500)
    private String website;

    @Column(name = "business_hours", columnDefinition = "TEXT")
    private String businessHours;

    public void updateFromTourApiData(Restaurant newData) {
        if (newData == null) {
            return;
        }

        this.name = newData.getName();
        this.address = newData.getAddress();
        this.phone = newData.getPhone();
        this.latitude = newData.getLatitude();
        this.longitude = newData.getLongitude();
        this.category = newData.getCategory();
        this.lastSyncedAt = LocalDateTime.now();
    }

    public boolean hasDataChangedFrom(Restaurant newData) {
        if (newData == null) {
            return false;
        }

        return !Objects.equals(this.name, newData.getName()) ||
                !Objects.equals(this.address, newData.getAddress()) ||
                !Objects.equals(this.phone, newData.getPhone()) ||
                !Objects.equals(this.latitude, newData.getLatitude()) ||
                !Objects.equals(this.longitude, newData.getLongitude()) ||
                !Objects.equals(this.category, newData.getCategory());
    }

    public void updateSyncTime() {
        this.lastSyncedAt = LocalDateTime.now();
    }

}
