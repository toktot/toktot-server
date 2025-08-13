package com.toktot.domain.restaurant;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Builder
@Entity
@Table(name = "restaurant_api_data",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_restaurant_api_type", columnNames = {"restaurant_id", "api_type"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RestaurantApiData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "api_type", nullable = false, length = 30)
    private String apiType;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "json")
    private String rawData;

    @Column(name = "business_hours", columnDefinition = "TEXT")
    private String businessHours;

    @Column(length = 200)
    private String website;

    @Column(name = "place_url", length = 300)
    private String placeUrl;

    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
