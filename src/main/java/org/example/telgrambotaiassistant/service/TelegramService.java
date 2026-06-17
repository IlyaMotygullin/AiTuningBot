package org.example.telgrambotaiassistant.service;

import org.example.telgrambotaiassistant.bot.Bot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class TelegramService {

    private final RestTemplate restTemplateTelegramApi;
    private final String botToken;

    public TelegramService(
            @Qualifier("restTemplateTelegramApi") RestTemplate restTemplateTelegramApi,
            @Value("${token_bot}") String botToken
    ) {
        this.restTemplateTelegramApi = restTemplateTelegramApi;
        this.botToken = botToken;
    }

    public Optional<PhotoPayload> extractLargestPhoto(Message message) {
        if (message == null) {
            return Optional.empty();
        }
        if (message.hasPhoto()) {
            List<PhotoSize> photos = message.getPhoto();
            PhotoSize largest = photos.stream()
                    .max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElse(photos.get(photos.size() - 1));
            return Optional.of(new PhotoPayload(largest.getFileId(), "image/jpeg"));
        }
        if (message.hasDocument()) {
            var document = message.getDocument();
            String mime = document.getMimeType();
            if (mime != null && mime.startsWith("image/")) {
                return Optional.of(new PhotoPayload(document.getFileId(), mime));
            }
        }
        return Optional.empty();
    }

    public byte[] downloadFile(Bot bot, String fileId) throws TelegramApiException {
        org.telegram.telegrambots.meta.api.objects.File telegramFile = bot.execute(new GetFile(fileId));
        String filePath = telegramFile.getFilePath();
        String url = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;

        ResponseEntity<byte[]> response = restTemplateTelegramApi.exchange(
                url, HttpMethod.GET, null, byte[].class
        );
        if (response.getBody() == null || response.getBody().length == 0) {
            throw new IllegalStateException("Telegram вернул пустой файл");
        }
        return response.getBody();
    }

    public void sendText(Bot bot, long chatId, String text) {
        sendText(bot, chatId, text, null);
    }

    public void sendText(Bot bot, long chatId, String text, ReplyKeyboard keyboard) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }
        execute(bot, message);
    }

    public void sendTextWithInline(Bot bot, long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
        execute(bot, message);
    }

    public void sendPhotoBytes(Bot bot, long chatId, byte[] image, String caption, InlineKeyboardMarkup keyboard) {
        SendPhoto photo = SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(new ByteArrayInputStream(image), "fitting.jpg"))
                .caption(caption)
                .replyMarkup(keyboard)
                .build();
        execute(bot, photo);
    }

    private void execute(Bot bot, SendMessage message) {
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            throw new IllegalStateException("Не удалось отправить сообщение в Telegram", e);
        }
    }

    private void execute(Bot bot, SendPhoto photo) {
        try {
            bot.execute(photo);
        } catch (TelegramApiException e) {
            throw new IllegalStateException("Не удалось отправить фото в Telegram", e);
        }
    }

    public record PhotoPayload(String fileId, String mimeType) {
    }
}
