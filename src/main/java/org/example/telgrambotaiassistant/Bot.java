package org.example.telgrambotaiassistant;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Bot extends TelegramLongPollingBot {
    String nameBot;

    public Bot(String botToken, String nameBot) {
        super(botToken);
        this.nameBot = nameBot;
    }

    public void sendText(Long id, String msg) {
        SendMessage sendMessage = SendMessage
                .builder()
                .chatId(id)
                .text(msg)
                .build();
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        String sendMsg = update.getMessage().getText();
        Long id = update.getMessage().getChatId();
        this.sendText(id, sendMsg);
    }

    @Override
    public String getBotUsername() {
        return nameBot;
    }
}
