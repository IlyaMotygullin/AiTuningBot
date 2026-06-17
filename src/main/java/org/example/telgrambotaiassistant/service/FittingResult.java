package org.example.telgrambotaiassistant.service;

public record FittingResult(boolean success, byte[] imageBytes, String message) {

    public static FittingResult ok(byte[] imageBytes) {
        return new FittingResult(true, imageBytes, "OK");
    }

    public static FittingResult fail(String message) {
        return new FittingResult(false, null, message);
    }
}
