package com.toktot.config.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account-key-base64}")
    private String firebaseKeyBase64;

    @PostConstruct
    public void initialize() {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(firebaseKeyBase64);
            ByteArrayInputStream serviceAccount = new ByteArrayInputStream(decodedKey);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase Admin SDK 초기화 완료");
            }
        } catch (IOException e) {
            log.error("❌ Firebase 초기화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Firebase 초기화 실패", e);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}
