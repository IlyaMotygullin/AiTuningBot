package org.example.telgrambotaiassistant.bot;

import org.example.telgrambotaiassistant.service.BotFlowService;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public class Bot extends TelegramLongPollingBot {

    private final String nameBot;
    private final BotFlowService botFlowService;

    public Bot(String botToken, String nameBot, BotFlowService botFlowService) {
        super(telegramOptions(), botToken);
        this.nameBot = nameBot;
        this.botFlowService = botFlowService;
    }

    private static DefaultBotOptions telegramOptions() {
        DefaultBotOptions options = new DefaultBotOptions();
        options.setProxyType(DefaultBotOptions.ProxyType.NO_PROXY);
        return options;
    }

    @Override
    public void onUpdateReceived(Update update) {
        botFlowService.handle(this, update);
    }

    @Override
    public String getBotUsername() {
        return nameBot;
    }
}
