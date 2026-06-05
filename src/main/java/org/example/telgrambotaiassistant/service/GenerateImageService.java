package org.example.telgrambotaiassistant.service;



import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.example.telgrambotaiassistant.client.ProxyApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerateImageService {
    @Autowired
    ProxyApiClient proxyApiClient;
    @Value(value = "${provider}")
    String provider;
    @Value(value = "${model_generation}")
    String model;
    final Logger LOGGER_SYSTEM_SERVICE = Logger.getLogger(GenerateImageService.class.getName());

    private Map<String, Object> createBody(String base64codeImageFirst, String base64codeImageSecond, String prompt) {
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("inline_data", Map.of(
                                                "mime_type",
                                                "image/jpeg",
                                                "data",
                                                base64codeImageFirst
                                        )
                                ),
                                Map.of("inline_data", Map.of(
                                                "mime_type",
                                                "image/jpeg",
                                                "data",
                                                base64codeImageSecond
                                        )
                                ),
                                Map.of("text", prompt)
                        ))
                )
        );
    }

    public String generateRequest(String imgFirst, String imgSecond, String prompt) {
        try {
            String uri = "/" + provider + "/v1beta/models/" + model + ":generateContent";
            Map<String, Object> body = createBody(imgFirst, imgSecond, prompt);
            LOGGER_SYSTEM_SERVICE.log(Level.INFO, "Выполняется запрос на api ии-модели {0}", uri);
            String response = proxyApiClient.post(uri, body);
            LOGGER_SYSTEM_SERVICE.log(Level.INFO, "Получен ответ от сервера ии-модели: {0}", response.length());
            return response;
        } catch (Exception e) {
            LOGGER_SYSTEM_SERVICE.log(Level.SEVERE, "Ошибка запроса к ии-модели: ", e);
            throw new RuntimeException(e);
        }
    }
}
