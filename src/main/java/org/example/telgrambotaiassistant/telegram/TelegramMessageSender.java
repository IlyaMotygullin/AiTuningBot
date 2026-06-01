package org.example.telgrambotaiassistant.telegram;

import org.example.telgrambotaiassistant.Bot;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;

@Service
public class TelegramMessageSender {

    private final Bot bot;

    public TelegramMessageSender(Bot bot) {
        this.bot = bot;
    }

    public void sendText(long chatId, String text) {
        sendText(chatId, text, null);
    }

    public void sendText(long chatId, String text, ReplyKeyboard keyboard) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }
        execute(message);
    }

    public void sendTextWithInline(long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
        execute(message);
    }

    public void sendPhotoBytes(long chatId, byte[] image, String caption, InlineKeyboardMarkup keyboard) {
        SendPhoto photo = SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(new ByteArrayInputStream(image), "fitting.jpg"))
                .caption(caption)
                .replyMarkup(keyboard)
                .build();
        execute(photo);
    }

    public void editText(long chatId, int messageId, String text, InlineKeyboardMarkup keyboard) {
        EditMessageText edit = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
        execute(edit);
    }

    public void removeKeyboard(long chatId, String text) {
        sendText(chatId, text, new ReplyKeyboardRemove(true));
    }

    private void execute(SendMessage message) {
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            throw new IllegalStateException("Не удалось отправить сообщение в Telegram", e);
        }
    }

    private void execute(SendPhoto photo) {
        try {
            bot.execute(photo);
        } catch (TelegramApiException e) {
            throw new IllegalStateException("Не удалось отправить фото в Telegram", e);
        }
    }

    private void execute(EditMessageText edit) {
        try {
            bot.execute(edit);
        } catch (TelegramApiException e) {
            throw new IllegalStateException("Не удалось обновить сообщение в Telegram", e);
        }
    }
}
