package com.exit.gateway.global.config;

import com.exit.gateway.global.resolver.DeviceIdArgumentResolver;
import com.exit.gateway.global.resolver.UserIdArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final DeviceIdArgumentResolver deviceIdArgumentResolver;
    private final UserIdArgumentResolver userIdArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(deviceIdArgumentResolver);
        resolvers.add(userIdArgumentResolver);
    }
}
