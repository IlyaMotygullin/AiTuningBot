package org.example.telgrambotaiassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "bot")
public class BotProperties {

    private Ai ai = new Ai();

    @Data
    public static class Ai {
        private boolean enabled = true;
        private String apiKey = "";
        private String baseUrl = "https://api.proxyapi.ru/google";
        private String validationModel = "gemini-2.5-flash";
        private String generationModel = "gemini-2.5-flash-image";
        private int maxRetries = 0;
        private long maxRetryDelayMs = 10_000;
    }
}
