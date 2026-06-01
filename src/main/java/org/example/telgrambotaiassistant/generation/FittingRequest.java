package org.example.telgrambotaiassistant.generation;

import org.example.telgrambotaiassistant.catalog.WheelCatalogItem;

public record FittingRequest(
        byte[] carImageBytes,
        byte[] wheelImageBytes,
        WheelCatalogItem catalogWheel,
        String prompt
) {
}
