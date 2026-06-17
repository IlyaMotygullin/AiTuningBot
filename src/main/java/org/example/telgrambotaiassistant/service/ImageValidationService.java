package org.example.telgrambotaiassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telgrambotaiassistant.client.ProxyApiClient;
import org.example.telgrambotaiassistant.config.BotProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageValidationService {

    private static final Pattern VALID_JSON = Pattern.compile("\"valid\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    private static final int MIN_BYTES = 5_000;
    private static final int MAX_BYTES = 15 * 1024 * 1024;

    private final BotProperties botProperties;
    private final ProxyApiClient proxyApiClient;
    private final ObjectMapper objectMapper;

    public ValidationResult validate(byte[] imageBytes, String mimeType, ImageType type) {
        ValidationResult basic = validateBasic(imageBytes, mimeType);
        if (!basic.valid()) {
            return basic;
        }
        if (!botProperties.getAi().isEnabled()) {
            return ValidationResult.ok();
        }
        if (botProperties.getAi().getApiKey() == null || botProperties.getAi().getApiKey().isBlank()) {
            return ValidationResult.fail("Не настроен API-ключ ProxyAPI (bot.ai.api-key).");
        }

        int maxAttempts = Math.max(1, botProperties.getAi().getMaxRetries() + 1);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return validateWithGemini(imageBytes, mimeType, type);
            } catch (RestClientException ex) {
                boolean isQuota = proxyApiClient.isQuotaError(ex);
                if (isQuota && attempt < maxAttempts) {
                    long delayMs = Math.min(parseRetryDelayMs(ex), botProperties.getAi().getMaxRetryDelayMs());
                    log.info("ProxyAPI 429, повтор {}/{} через {} мс", attempt, maxAttempts - 1, delayMs);
                    sleep(delayMs);
                    continue;
                }
                return handleApiError(ex, isQuota);
            }
        }
        return ValidationResult.fail("Не удалось проверить фото. Попробуйте позже.");
    }

    private static ValidationResult validateBasic(byte[] imageBytes, String mimeType) {
        if (imageBytes == null || imageBytes.length < MIN_BYTES) {
            return ValidationResult.fail("Изображение слишком маленькое или пустое.");
        }
        if (imageBytes.length > MAX_BYTES) {
            return ValidationResult.fail("Файл больше 15 МБ. Сожмите фото и попробуйте снова.");
        }
        if (mimeType == null || !mimeType.startsWith("image/")) {
            return ValidationResult.fail("Нужен файл изображения (JPG, PNG или WEBP).");
        }
        return ValidationResult.ok();
    }

    private ValidationResult validateWithGemini(byte[] imageBytes, String mimeType, ImageType type) {
        String model = botProperties.getAi().getValidationModel();
        ObjectNode body = objectMapper.createObjectNode();
        body.set("contents", proxyApiClient.userContent(
                proxyApiClient.inlineImagePart(imageBytes, mimeType),
                proxyApiClient.textPart(GeminiPrompts.validationFor(type))
        ));
        ObjectNode generationConfig = objectMapper.createObjectNode();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("responseMimeType", "application/json");
        body.set("generationConfig", generationConfig);

        JsonNode response = proxyApiClient.generateContent(model, body);
        String blockReason = proxyApiClient.blockReasonMessage(response);
        if (!blockReason.isBlank()) {
            return ValidationResult.fail(blockReason);
        }

        String text = proxyApiClient.extractText(response);
        if (text.isBlank()) {
            return ValidationResult.fail("Модель не вернула ответ при проверке фото.");
        }
        return parseValidationAnswer(text);
    }

    private ValidationResult parseValidationAnswer(String text) {
        try {
            JsonNode json = objectMapper.readTree(stripMarkdown(text));
            boolean valid = json.path("valid").asBoolean(false);
            String message = json.path("message").asText(valid ? "OK" : "Фото не подходит.");
            return valid ? ValidationResult.ok() : ValidationResult.fail(message);
        } catch (Exception ignored) {
            Matcher matcher = VALID_JSON.matcher(text);
            if (matcher.find()) {
                boolean valid = Boolean.parseBoolean(matcher.group(1));
                return valid ? ValidationResult.ok() : ValidationResult.fail("Фото не подходит для этой операции.");
            }
            log.warn("Не удалось разобрать ответ валидации: {}", text);
            return ValidationResult.fail("Не удалось распознать ответ проверки. Попробуйте другое фото.");
        }
    }

    private static String stripMarkdown(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n');
            int end = trimmed.lastIndexOf("```");
            if (start >= 0 && end > start) {
                return trimmed.substring(start + 1, end).trim();
            }
        }
        return trimmed;
    }

    private ValidationResult handleApiError(RestClientException ex, boolean isQuota) {
        log.warn("Gemini validation failed: {}", ex.getMessage());
        if (proxyApiClient.isInsufficientBalance(ex)) {
            return ValidationResult.fail("На балансе ProxyAPI недостаточно средств. Пополните счёт на proxyapi.ru.");
        }
        if (isQuota) {
            return ValidationResult.fail("Исчерпана квота проверки фото. Попробуйте позже или пополните баланс ProxyAPI.");
        }
        if (proxyApiClient.isModelNotFound(ex)) {
            return ValidationResult.fail("Модель проверки недоступна. Укажите bot.ai.validation-model (например gemini-2.5-flash).");
        }
        if (ex instanceof HttpStatusCodeException httpEx) {
            int code = httpEx.getStatusCode().value();
            if (code == 401 || code == 403) {
                return ValidationResult.fail("Неверный API-ключ ProxyAPI. Проверьте bot.ai.api-key.");
            }
        }
        return ValidationResult.fail("Сервис проверки фото временно недоступен. Попробуйте позже.");
    }

    private static long parseRetryDelayMs(RestClientException ex) {
        if (!(ex instanceof HttpStatusCodeException httpEx)) {
            return 2_000;
        }
        String body = httpEx.getResponseBodyAsString();
        if (body == null) {
            return 2_000;
        }
        Matcher matcher = Pattern.compile("retry in (\\d+(?:\\.\\d+)?)s", Pattern.CASE_INSENSITIVE).matcher(body);
        if (matcher.find()) {
            return (long) (Double.parseDouble(matcher.group(1)) * 1000);
        }
        return 2_000;
    }

    private static void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
