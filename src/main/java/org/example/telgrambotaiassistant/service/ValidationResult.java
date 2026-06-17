package org.example.telgrambotaiassistant.service;

public record ValidationResult(boolean valid, String message) {

    public static ValidationResult ok() {
        return new ValidationResult(true, "OK");
    }

    public static ValidationResult fail(String message) {
        return new ValidationResult(false, message);
    }
}
