package org.example.telgrambotaiassistant.generation;

import org.springframework.stereotype.Service;

/** Заглушка генерации: возвращает фото авто. */
@Service
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
