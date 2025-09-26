package com.exit.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.bootstrap.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "server.port=0",
        "jwt.secret=test-secret-key-for-unit-testing",
        "jwt.access-token-expiration=86400000",
        "jwt.refresh-token-expiration=604800000",
        "oauth2.kakao.client-id=test",
        "oauth2.kakao.client-secret=test",
        "oauth2.naver.client-id=test",
        "oauth2.naver.client-secret=test",
        "redis.host=localhost",
        "redis.port=6379"
})
class UserApplicationTests {

    @Test
    void contextLoads() {
    }

}
