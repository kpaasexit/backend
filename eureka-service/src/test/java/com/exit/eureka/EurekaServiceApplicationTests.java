package com.exit.eureka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.bootstrap.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "eureka.instance.hostname=localhost",
        "eureka.server.enable-self-preservation=false",
        "server.port=0"
})
class EurekaServiceApplicationTests {

    @Test
    void contextLoads() {
        // 단순 컨텍스트 로드 테스트
    }

}
