package com.exit.notification.config;

import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

@TestConfiguration
@Profile("test")
public class TestFirebaseConfig {

    @Bean
    @Primary
    public FirebaseMessaging firebaseMessaging() {
        return mock(FirebaseMessaging.class);
    }
}
