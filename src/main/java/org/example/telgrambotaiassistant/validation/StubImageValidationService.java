package org.example.telgrambotaiassistant.validation;

import org.springframework.stereotype.Service;

/** Заглушка: формат и размер файла. */
@Service
public class StubImageValidationService implements ImageValidationService {

    @Override
    public ValidationResult validate(byte[] imageBytes, String mimeType, ImageType type) {
        return BasicImageValidator.validate(imageBytes, mimeType);
    }
}
