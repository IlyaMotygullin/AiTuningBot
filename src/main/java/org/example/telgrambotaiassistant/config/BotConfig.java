package org.example.telgrambotaiassistant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.telgrambotaiassistant.bot.Bot;
import org.example.telgrambotaiassistant.service.BotFlowService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BotProperties.class)
public class BotConfig {

    @Value("${token_bot}")
    private String tokenBot;

    @Value("${name_bot}")
    private String nameBot;

    @Bean(name = "botBean")
    public Bot bot(BotFlowService botFlowService) {
        return new Bot(tokenBot, nameBot, botFlowService);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
