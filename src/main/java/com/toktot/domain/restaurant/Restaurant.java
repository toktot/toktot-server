package com.toktot.domain.restaurant;

import com.toktot.domain.restaurant.type.DataSource;
import com.toktot.external.kakao.dto.response.KakaoPlaceInfo;
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

    @Setter
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

    @Setter
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
    @Setter
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

    @Setter
    @Column(name = "image", length = 500)
    private String image;

    @Builder.Default
    @Column(name = "is_local_store", nullable = false)
    private Boolean isLocalStore = false;

    @Column(length = 500)
    private String website;

    @Setter
    @Column(name = "business_hours")
    private String businessHours;

    @Setter
    @Column(name = "popular_menus")
    private String popularMenus;

    public void updateKakaoData(KakaoPlaceInfo k) {
        this.externalKakaoId = k.getId();
        this.category = k.getCategoryGroupName();
        this.phone = k.getPhone();
        this.address = k.getAddressName();
        this.roadAddress = k.getRoadAddressName();
        this.latitude = k.getLatitude();
        this.longitude = k.getLongitude();
        this.website = k.getPlaceUrl();
    }

    public void updateTourApiDetailCommon(String phone, String image, String image2) {
        if (phone != null && !phone.trim().isEmpty() && !phone.equals(this.phone)) {
                this.phone = phone;
            }

        if (image != null && !image.trim().isEmpty() && !image.equals(this.image)) {
            this.image = image;
        } else if (image2 != null && !image2.trim().isEmpty() && !image2.equals(this.image)) {
            this.image = image2;
        }
    }
}
