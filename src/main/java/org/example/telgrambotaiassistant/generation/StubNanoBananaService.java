package org.example.telgrambotaiassistant.generation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Заглушка генерации (когда bot.ai.enabled=false). */
@Service
@ConditionalOnProperty(prefix = "bot.ai", name = "enabled", havingValue = "false", matchIfMissing = true)
public class StubNanoBananaService implements NanoBananaService {

    @Override
    public FittingResult generateFitting(FittingRequest request) {
        if (request.carImageBytes() == null || request.carImageBytes().length == 0) {
            return FittingResult.fail("Нет фото автомобиля.");
        }
        boolean hasWheel = request.wheelImageBytes() != null && request.wheelImageBytes().length > 0;
        boolean hasCatalog = request.catalogWheel() != null;
        if (!hasWheel && !hasCatalog) {
            return FittingResult.fail("Не выбраны диски для примерки.");
        }
        return FittingResult.ok(request.carImageBytes());
    }
}
