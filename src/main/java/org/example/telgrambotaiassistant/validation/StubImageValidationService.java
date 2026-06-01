package org.example.telgrambotaiassistant.validation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Заглушка: формат и размер файла (когда bot.ai.enabled=false). */
@Service
@ConditionalOnProperty(prefix = "bot.ai", name = "enabled", havingValue = "false", matchIfMissing = true)
public class StubImageValidationService implements ImageValidationService {

    @Override
    public ValidationResult validate(byte[] imageBytes, String mimeType, ImageType type) {
        return BasicImageValidator.validate(imageBytes, mimeType);
    }
}
