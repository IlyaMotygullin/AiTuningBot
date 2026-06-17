package org.example.telgrambotaiassistant.service;

public record FittingRequest(
        byte[] carImageBytes,
        byte[] wheelImageBytes,
        WheelCatalogItem catalogWheel,
        String prompt
) {
}
