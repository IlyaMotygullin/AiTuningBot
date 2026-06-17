package org.example.telgrambotaiassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telgrambotaiassistant.client.ProxyApiClient;
import org.example.telgrambotaiassistant.config.BotProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateImageService {

    private final BotProperties botProperties;
    private final ProxyApiClient proxyApiClient;
    private final ObjectMapper objectMapper;

    public FittingResult generateFitting(FittingRequest request) {
        if (request.carImageBytes() == null || request.carImageBytes().length == 0) {
            return FittingResult.fail("Нет фото автомобиля.");
        }
        boolean hasWheel = request.wheelImageBytes() != null && request.wheelImageBytes().length > 0;
        boolean hasCatalog = request.catalogWheel() != null;
        if (!hasWheel && !hasCatalog) {
            return FittingResult.fail("Не выбраны диски для примерки.");
        }

        if (!botProperties.getAi().isEnabled()) {
            return FittingResult.ok(request.carImageBytes());
        }
        if (botProperties.getAi().getApiKey() == null || botProperties.getAi().getApiKey().isBlank()) {
            return FittingResult.fail("Не настроен API-ключ ProxyAPI (bot.ai.api-key).");
        }

        try {
            return generateWithGemini(request);
        } catch (RestClientException ex) {
            log.warn("Gemini fitting failed: {}", ex.getMessage());
            if (proxyApiClient.isInsufficientBalance(ex)) {
                return FittingResult.fail("На балансе ProxyAPI недостаточно средств. Пополните счёт на proxyapi.ru.");
            }
            if (proxyApiClient.isQuotaError(ex)) {
                return FittingResult.fail("Исчерпана квота генерации. Попробуйте позже или пополните баланс ProxyAPI.");
            }
            if (proxyApiClient.isModelNotFound(ex)) {
                return FittingResult.fail("Модель генерации недоступна. Проверьте bot.ai.generation-model.");
            }
            if (ex instanceof HttpStatusCodeException httpEx) {
                int code = httpEx.getStatusCode().value();
                if (code == 401 || code == 403) {
                    return FittingResult.fail("Неверный API-ключ ProxyAPI.");
                }
            }
            return FittingResult.fail("Не удалось сгенерировать примерку. Попробуйте позже.");
        }
    }

    private FittingResult generateWithGemini(FittingRequest request) {
        String model = botProperties.getAi().getGenerationModel();
        List<ObjectNode> parts = new ArrayList<>();
        parts.add(proxyApiClient.inlineImagePart(request.carImageBytes(), "image/jpeg"));
        if (request.wheelImageBytes() != null && request.wheelImageBytes().length > 0) {
            parts.add(proxyApiClient.inlineImagePart(request.wheelImageBytes(), "image/jpeg"));
        }
        parts.add(proxyApiClient.textPart(GeminiPrompts.fitting(request)));

        ObjectNode body = objectMapper.createObjectNode();
        body.set("contents", proxyApiClient.userContent(parts.toArray(ObjectNode[]::new)));

        ObjectNode generationConfig = objectMapper.createObjectNode();
        ArrayNode modalities = objectMapper.createArrayNode();
        modalities.add("IMAGE");
        generationConfig.set("responseModalities", modalities);
        body.set("generationConfig", generationConfig);

        JsonNode response = proxyApiClient.generateContent(model, body);
        String blockReason = proxyApiClient.blockReasonMessage(response);
        if (!blockReason.isBlank()) {
            return FittingResult.fail(blockReason);
        }

        Optional<byte[]> image = proxyApiClient.extractImageBytes(response);
        if (image.isPresent()) {
            return FittingResult.ok(image.get());
        }

        String text = proxyApiClient.extractText(response);
        if (!text.isBlank()) {
            log.warn("Модель вернула текст вместо изображения: {}", text.length() > 200 ? text.substring(0, 200) + "…" : text);
        }
        return FittingResult.fail("Модель не вернула изображение примерки. Попробуйте ещё раз.");
    }
}
