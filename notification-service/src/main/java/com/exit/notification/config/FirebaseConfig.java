package com.exit.notification.config;

import com.google.auth.oauth2.ServiceAccountCredentials;
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
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.project-id}")
    private String projectId;

    @Value("${firebase.private-key-id}")
    private String privateKeyId;

    @Value("${firebase.private-key}")
    private String privateKey;

    @Value("${firebase.client-email}")
    private String clientEmail;

    @Value("${firebase.client-id}")
    private String clientId;

    @Value("${firebase.auth-uri}")
    private String authUri;

    @Value("${firebase.token-uri}")
    private String tokenUri;

    @Value("${firebase.auth-provider-x509-cert-url}")
    private String authProviderX509CertUrl;

    @Value("${firebase.client-x509-cert-url}")
    private String clientX509CertUrl;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                String serviceAccountJson = createServiceAccountJson();

                ServiceAccountCredentials credentials = ServiceAccountCredentials
                        .fromStream(new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8)));

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .setProjectId(projectId)
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase application has been initialized with project: {}", projectId);
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase", e);
        }
    }

    private String createServiceAccountJson() {
        return String.format("""
                {
                  "type": "service_account",
                  "project_id": "%s",
                  "private_key_id": "%s",
                  "private_key": "%s",
                  "client_email": "%s",
                  "client_id": "%s",
                  "auth_uri": "%s",
                  "token_uri": "%s",
                  "auth_provider_x509_cert_url": "%s",
                  "client_x509_cert_url": "%s"
                }
                """, projectId, privateKeyId, privateKey, clientEmail, clientId,
                authUri, tokenUri, authProviderX509CertUrl, clientX509CertUrl);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}