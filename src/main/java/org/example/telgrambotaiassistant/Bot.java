package org.example.telgrambotaiassistant;

import lombok.extern.slf4j.Slf4j;
import org.example.telgrambotaiassistant.handler.UpdateHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
public class Bot extends TelegramLongPollingBot {

    private final String nameBot;
    private final UpdateHandler updateHandler;

    public Bot(
            @Value("${token_bot}") String botToken,
            @Value("${name_bot}") String nameBot,
            @Lazy UpdateHandler updateHandler
    ) {
        super(telegramOptions(), botToken);
        this.nameBot = nameBot;
        this.updateHandler = updateHandler;
    }

    private static DefaultBotOptions telegramOptions() {
        DefaultBotOptions options = new DefaultBotOptions();
        options.setProxyType(DefaultBotOptions.ProxyType.NO_PROXY);
        return options;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            updateHandler.handle(update);
        } catch (Exception ex) {
            log.error("Ошибка обработки update", ex);
        }
    }

    @Override
    public String getBotUsername() {
        return nameBot;
    }
}
