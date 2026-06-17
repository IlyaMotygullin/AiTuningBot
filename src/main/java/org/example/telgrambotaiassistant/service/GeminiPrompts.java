package org.example.telgrambotaiassistant.service;

/**
 * Промпты из ветки Larik2 (GeminiImageValidationService + GeminiNanoBananaService).
 */
public final class GeminiPrompts {

    private GeminiPrompts() {
    }

    public static String validationCar() {
        return """
                Ты модератор фото для сервиса виртуальной примерки дисков на автомобиль.
                На снимке должен быть автомобиль целиком или почти целиком (вид сбоку/3/4), не логотип, не интерьер, не только колесо.
                Ответь строго JSON без markdown: {"valid": true или false, "message": "кратко по-русски почему"}
                """;
    }

    public static String validationWheel() {
        return """
                Ты модератор фото дисков для автосервиса.
                На снимке должен быть автомобильный диск (колёсный диск), желательно крупным планом; не автомобиль целиком, не скриншот каталога без диска.
                Ответь строго JSON без markdown: {"valid": true или false, "message": "кратко по-русски почему"}
                """;
    }

    public static String validationFor(ImageType type) {
        return type == ImageType.CAR ? validationCar() : validationWheel();
    }

    public static String fitting(FittingRequest request) {
        WheelCatalogItem catalog = request.catalogWheel();
        String wheelDescription;
        if (catalog != null) {
            wheelDescription = "диски из каталога: " + catalog.getTitle();
        } else {
            wheelDescription = "диски со второго приложенного фото (референс)";
        }
        String custom = request.prompt() != null && !request.prompt().isBlank()
                ? "\nДополнительно: " + request.prompt()
                : "";
        return """
                Ты редактор фото для автотюнинга. На первом изображении — автомобиль.
                Реалистично установи на все видимые колёса %s.
                Сохрани ракурс, освещение, фон и пропорции кузова. Диски должны выглядеть естественно.
                Верни только итоговое фото автомобиля с новыми дисками.%s
                """.formatted(wheelDescription, custom);
    }
}
