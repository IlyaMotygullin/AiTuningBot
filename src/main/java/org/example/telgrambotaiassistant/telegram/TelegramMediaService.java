package org.example.telgrambotaiassistant.telegram;

import org.example.telgrambotaiassistant.Bot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class TelegramMediaService {

    private final Bot bot;
    private final RestTemplate telegramRestTemplate;

    public TelegramMediaService(
            Bot bot,
            @Qualifier("telegramRestTemplate") RestTemplate telegramRestTemplate
    ) {
        this.bot = bot;
        this.telegramRestTemplate = telegramRestTemplate;
    }

    @Value("${token_bot}")
    private String botToken;

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

    public byte[] downloadFile(String fileId) throws TelegramApiException {
        org.telegram.telegrambots.meta.api.objects.File telegramFile = bot.execute(new GetFile(fileId));
        String filePath = telegramFile.getFilePath();
        String url = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;

        ResponseEntity<byte[]> response = telegramRestTemplate.exchange(
                url, HttpMethod.GET, null, byte[].class
        );
        if (response.getBody() == null || response.getBody().length == 0) {
            throw new IllegalStateException("Telegram вернул пустой файл");
        }
        return response.getBody();
    }

    public record PhotoPayload(String fileId, String mimeType) {
    }
}
