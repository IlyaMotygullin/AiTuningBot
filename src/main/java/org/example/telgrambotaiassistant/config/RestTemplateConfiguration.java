package org.example.telgrambotaiassistant.config;



import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RestTemplateConfiguration {
    @Bean(value = "restTemplateTelegramApi")
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(100))
                .readTimeout(Duration.ofSeconds(50))
                .build();
    }
}
