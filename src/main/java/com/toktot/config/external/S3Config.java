package com.toktot.config.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
@Configuration
public class S3Config {

    @Value("${aws.credentials.access-key}")
    private String accessKeyId;

    @Value("${aws.credentials.secret-key}")
    private String secretAccessKey;

    @Value("${toktot.s3.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        log.info("AWS S3 Client 초기화 시작 - region: {}", region);

        try {
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

            S3Client s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .build();

            log.info("AWS S3 Client 초기화 완료");
            return s3Client;

        } catch (Exception e) {
            log.error("AWS S3 Client 초기화 실패 - error: {}", e.getMessage(), e);
            throw new IllegalStateException("AWS S3 설정 실패", e);
        }
    }
}
