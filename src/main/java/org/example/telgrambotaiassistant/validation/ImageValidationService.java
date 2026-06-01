package org.example.telgrambotaiassistant.validation;

public interface ImageValidationService {

    ValidationResult validate(byte[] imageBytes, String mimeType, ImageType type);
}
