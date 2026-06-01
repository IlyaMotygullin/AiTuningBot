package org.example.telgrambotaiassistant.validation;

final class BasicImageValidator {

    private static final int MIN_BYTES = 5_000;
    private static final int MAX_BYTES = 15 * 1024 * 1024;

    private BasicImageValidator() {
    }

    static ValidationResult validate(byte[] imageBytes, String mimeType) {
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
}
