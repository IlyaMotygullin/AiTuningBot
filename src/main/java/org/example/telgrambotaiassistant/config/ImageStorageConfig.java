package org.example.telgrambotaiassistant.config;

import org.example.telgrambotaiassistant.bot.ImageStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImageStorageConfig {
    @Bean(value = "imageStorageBean")
    public ImageStorage imageStorage() {
        return new ImageStorage();
    }
}
