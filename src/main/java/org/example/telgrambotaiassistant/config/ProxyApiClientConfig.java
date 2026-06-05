package org.example.telgrambotaiassistant.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.telgrambotaiassistant.client.ProxyApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProxyApiClientConfig {

    @Bean
    public ProxyApiClient proxyApiClient() {
        return new ProxyApiClient();
    }
}
