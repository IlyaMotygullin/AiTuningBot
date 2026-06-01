package org.example.telgrambotaiassistant.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.example.telgrambotaiassistant.catalog.WheelCatalogItem;
import org.example.telgrambotaiassistant.configuration.BotProperties;
import org.example.telgrambotaiassistant.gemini.ProxyApiGeminiClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "bot.ai", name = "enabled", havingValue = "true")
public class GeminiNanoBananaService implements NanoBananaService {

    private final BotProperties botProperties;
    private final ProxyApiGeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public GeminiNanoBananaService(
            BotProperties botProperties,
            ProxyApiGeminiClient geminiClient,
            ObjectMapper objectMapper
    ) {
        this.botProperties = botProperties;
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public FittingResult generateFitting(FittingRequest request) {
        if (request.carImageBytes() == null || request.carImageBytes().length == 0) {
            return FittingResult.fail("Нет фото автомобиля.");
        }
        boolean hasWheel = request.wheelImageBytes() != null && request.wheelImageBytes().length > 0;
        boolean hasCatalog = request.catalogWheel() != null;
        if (!hasWheel && !hasCatalog) {
            return FittingResult.fail("Не выбраны диски для примерки.");
        }
        if (geminiClient.resolveApiKey() == null) {
            return FittingResult.fail("Не настроен API-ключ ProxyAPI (bot.ai.api-key).");
        }

        try {
            return generateWithGemini(request);
        } catch (RestClientException ex) {
            log.warn("Gemini fitting failed: {}", ex.getMessage());
            if (geminiClient.isInsufficientBalance(ex)) {
                return FittingResult.fail("На балансе ProxyAPI недостаточно средств. Пополните счёт на proxyapi.ru.");
            }
            if (geminiClient.isQuotaError(ex)) {
                return FittingResult.fail("Исчерпана квота генерации. Попробуйте позже или пополните баланс ProxyAPI.");
            }
            if (geminiClient.isModelNotFound(ex)) {
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
        parts.add(geminiClient.inlineImagePart(request.carImageBytes(), "image/jpeg"));
        if (request.wheelImageBytes() != null && request.wheelImageBytes().length > 0) {
            parts.add(geminiClient.inlineImagePart(request.wheelImageBytes(), "image/jpeg"));
        }
        parts.add(geminiClient.textPart(buildPrompt(request)));

        ObjectNode body = objectMapper.createObjectNode();
        body.set("contents", geminiClient.userContent(parts.toArray(ObjectNode[]::new)));

        ObjectNode generationConfig = objectMapper.createObjectNode();
        ArrayNode modalities = objectMapper.createArrayNode();
        modalities.add("IMAGE");
        generationConfig.set("responseModalities", modalities);
        body.set("generationConfig", generationConfig);

        JsonNode response = geminiClient.generateContent(model, body);
        String blockReason = geminiClient.blockReasonMessage(response);
        if (!blockReason.isBlank()) {
            return FittingResult.fail(blockReason);
        }

        Optional<byte[]> image = geminiClient.extractImageBytes(response);
        if (image.isPresent()) {
            return FittingResult.ok(image.get());
        }

        String text = geminiClient.extractText(response);
        if (!text.isBlank()) {
            log.warn("Модель вернула текст вместо изображения: {}", text.length() > 200 ? text.substring(0, 200) + "…" : text);
        }
        return FittingResult.fail("Модель не вернула изображение примерки. Попробуйте ещё раз.");
    }

    private static String buildPrompt(FittingRequest request) {
        WheelCatalogItem catalog = request.catalogWheel();
        String wheelDescription;
        if (catalog != null) {
            wheelDescription = "диски из каталога: " + catalog.getTitle();
        } else {
            wheelDescription = "диски со второго приложенного фото (референс)";
        }
        String custom = request.prompt() != null && !request.prompt().isBlank()
                ? "\nДополнительно: " + request.prompt()
                : "";
        return """
                Ты редактор фото для автотюнинга. На первом изображении — автомобиль.
                Реалистично установи на все видимые колёса %s.
                Сохрани ракурс, освещение, фон и пропорции кузова. Диски должны выглядеть естественно.
                Верни только итоговое фото автомобиля с новыми дисками.%s
                """.formatted(wheelDescription, custom);
    }
}
