package org.example.telgrambotaiassistant.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Base64;
import java.util.Optional;

@Component
public class ProxyApiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ProxyApiClient(
            @Qualifier("restClientBean") RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public JsonNode generateContent(String model, ObjectNode body) {
        String response = restClient.post()
                .uri("/v1beta/models/{model}:generateContent", model)
                .body(body.toString())
                .retrieve()
                .body(String.class);
        try {
            return objectMapper.readTree(response);
        } catch (Exception ex) {
            throw new RestClientException("Не удалось разобрать ответ Gemini: " + ex.getMessage(), ex);
        }
    }

    public Optional<byte[]> extractImageBytes(JsonNode root) {
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray()) {
            return Optional.empty();
        }
        for (JsonNode candidate : candidates) {
            JsonNode parts = candidate.path("content").path("parts");
            if (!parts.isArray()) {
                continue;
            }
            for (JsonNode part : parts) {
                JsonNode inlineData = part.path("inlineData");
                if (inlineData.isMissingNode()) {
                    inlineData = part.path("inline_data");
                }
                if (!inlineData.isMissingNode() && inlineData.has("data")) {
                    return Optional.of(Base64.getDecoder().decode(inlineData.get("data").asText()));
                }
            }
        }
        return Optional.empty();
    }

    public String extractText(JsonNode root) {
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return "";
        }
        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.has("text")) {
                sb.append(part.get("text").asText());
            }
        }
        return sb.toString().trim();
    }

    public String blockReasonMessage(JsonNode root) {
        JsonNode promptFeedback = root.path("promptFeedback");
        if (promptFeedback.has("blockReason")) {
            return "Запрос отклонён модерацией: " + promptFeedback.get("blockReason").asText();
        }
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            String reason = candidates.get(0).path("finishReason").asText("");
            if (!reason.isBlank() && !"STOP".equals(reason)) {
                return "Генерация остановлена: " + reason;
            }
        }
        return "";
    }

    public ObjectNode inlineImagePart(byte[] imageBytes, String mimeType) {
        ObjectNode inlineData = objectMapper.createObjectNode();
        inlineData.put("mimeType", normalizeMimeType(mimeType));
        inlineData.put("data", Base64.getEncoder().encodeToString(imageBytes));

        ObjectNode part = objectMapper.createObjectNode();
        part.set("inlineData", inlineData);
        return part;
    }

    public ObjectNode textPart(String text) {
        ObjectNode part = objectMapper.createObjectNode();
        part.put("text", text);
        return part;
    }

    public ArrayNode userContent(ObjectNode... parts) {
        ArrayNode partsArray = objectMapper.createArrayNode();
        for (ObjectNode part : parts) {
            partsArray.add(part);
        }
        ObjectNode content = objectMapper.createObjectNode();
        content.put("role", "user");
        content.set("parts", partsArray);

        ArrayNode contents = objectMapper.createArrayNode();
        contents.add(content);
        return contents;
    }

    public boolean isQuotaError(RestClientException ex) {
        if (ex instanceof HttpStatusCodeException httpEx) {
            if (httpEx.getStatusCode().value() == 429) {
                return true;
            }
            String body = httpEx.getResponseBodyAsString();
            return body != null && (body.contains("RESOURCE_EXHAUSTED") || body.contains("quota"));
        }
        return false;
    }

    public boolean isInsufficientBalance(RestClientException ex) {
        if (!(ex instanceof HttpStatusCodeException httpEx)) {
            return false;
        }
        String body = httpEx.getResponseBodyAsString();
        return body != null && body.toLowerCase().contains("insufficient balance");
    }

    public boolean isModelNotFound(RestClientException ex) {
        if (ex instanceof HttpStatusCodeException httpEx) {
            if (httpEx.getStatusCode().value() == 404) {
                return true;
            }
            String body = httpEx.getResponseBodyAsString();
            return body != null && body.contains("NOT_FOUND") && body.contains("models/");
        }
        return false;
    }

    private static String normalizeMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "image/jpeg";
        }
        return switch (mimeType.toLowerCase()) {
            case "image/jpg" -> "image/jpeg";
            default -> mimeType;
        };
    }
}
