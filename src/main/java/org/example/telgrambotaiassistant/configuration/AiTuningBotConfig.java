package org.example.telgrambotaiassistant.configuration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.example.telgrambotaiassistant.Bot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiTuningBotConfig {
    @Value(value = "${token_bot}")
    String tokenBot;

    @Value(value = "${name_bot}")
    String nameBot;

    @Bean
    public Bot bot() {
        return new Bot(tokenBot, nameBot);
    }
}
