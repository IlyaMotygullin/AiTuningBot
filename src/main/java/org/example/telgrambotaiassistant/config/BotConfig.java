package org.example.telgrambotaiassistant.config;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.example.telgrambotaiassistant.bot.Bot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BotConfig {
    @Value(value = "${name_bot}")
    String nameBot;
    @Value(value = "${token_bot}")
    String tokenBot;

    @Bean(value = "botBean")
    public Bot bot() {
        return new Bot(tokenBot, nameBot);
    }
}
