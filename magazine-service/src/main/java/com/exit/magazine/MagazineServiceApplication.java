package com.exit.magazine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
@ComponentScan(basePackages = {
        "com.exit.magazine",    // magazine 패키지
        "com.exit.common"      // Common 패키지 추가
})
public class MagazineServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MagazineServiceApplication.class, args);
    }

}
