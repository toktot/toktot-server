package com.toktot.domain.restaurant;

import jakarta.persistence.*;
import lombok.*;

@Builder
@Entity
@Table(name = "restaurant_menu_submission_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RestaurantMenuSubmissionImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private RestaurantMenuSubmissions submission;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    public void assignSubmission(RestaurantMenuSubmissions submission) {
        this.submission = submission;
    }

    public static RestaurantMenuSubmissionImage create(String imageUrl) {
        return RestaurantMenuSubmissionImage.builder()
                .imageUrl(imageUrl)
                .build();
    }
}
