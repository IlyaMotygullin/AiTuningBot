package org.example.telgrambotaiassistant.service;

public final class BotMessages {

    public static final String WELCOME = """
            Добро пожаловать в Tuning AI Bot

            Представь, как будут выглядеть новые диски на твоём автомобиле ещё до покупки.

            • Загрузи фото
            • Выбери диски
            • Получи реалистичный результат за секунды

            Твой следующий стиль начинается здесь.""";

    public static final String CAR_UPLOAD_INSTRUCTION = """
            Загрузите фотографию автомобиля

            Для лучшего результата используйте фото, на котором хорошо видны все колёса автомобиля.

            • Боковой или полубоковой ракурс
            • Хорошее освещение
            • Автомобиль полностью в кадре
            • Без сильного размытия

            Поддерживаемые форматы: JPG, PNG, WEBP
            Обязательно загружайте фото автомобиля, иначе обработка не будет выполнена.

            Отправьте фотографию автомобиля одним сообщением.""";

    public static final String WHEEL_UPLOAD_INSTRUCTION = """
            📸 Загрузите фотографию диска

            Для лучшего результата:

            ✅ Диск должен быть снят спереди
            ✅ Без сильных теней
            ✅ В хорошем качестве
            ✅ Желательно без автомобиля

            После загрузки AI автоматически подготовит диск к примерке.""";

    public static final String GENERATING = "Идёт процесс генерации, подождите несколько секунд…";

    public static final String PREMIUM_INACTIVE = """
            👑 Premium не активен

            Преимущества при покупке:
            ♾ Безлимитные генерации
            ⚡ Приоритетная очередь
            🚀 Быстрая обработка
            🎯 Максимальное качество""";

    public static final String PREMIUM_PAYMENT_STUB = "💳 Оплата Premium — в разработке. Скоро подключим оплату.";

    public static final String RESULTS_HEADER = "✨ Результаты\n\nПоследние 10 генераций.";

    public static final String RESULTS_EMPTY = "История пуста.\n\nСделайте первую примерку: загрузите авто и выберите диски.";

    public static final String PROFILE = """
            Ваш профиль

            Статус:
            Free

            Генераций всего:
            0

            За сегодня:
            0

            Среднее время:
            —""";

    public static final String SUPPORT = "📞 Поддержка: @DS_Ultimate";

    public static final String STUB_ACTION = "Функция в разработке — скоро подключим.";

    public static final String CATALOG_SHOW_MORE = "Доступные диски\n\nЕщё модели появятся в каталоге в следующем обновлении.";

    public static String carSaved(boolean alreadyHadCar) {
        if (alreadyHadCar) {
            return """
                    ✅ Новое фото автомобиля сохранено.

                    Теперь выберите диски из каталога или загрузите свои.""";
        }
        return """
                Автомобиль успешно загружен

                Теперь выберите диски из каталога и посмотрите, как они будут выглядеть на вашем автомобиле.

                Доступно более 20 вариантов дисков

                Нажмите «Загрузка Дисков» или кнопку «💿 Диски» в меню.""";
    }

    public static String carMenu(boolean hasCar) {
        if (!hasCar) {
            return "🚗 Авто\n\nАвтомобиль ещё не загружен. Отправьте фото или нажмите «📸 Загрузить новое авто».";
        }
        return """
                🚗 Авто

                Текущее авто
                Загружено: сегодня

                Можно загрузить новое фото или удалить текущее.""";
    }

    public static String diskMenuWithSavedCar() {
        return """
                💿 Диски

                Используем сохранённое фото автомобиля.
                Выберите диски из каталога или загрузите свои.""";
    }

    public static String diskMenuNeedCar() {
        return """
                💿 Диски

                Сначала нужно загрузить фото автомобиля.
                Нажмите «Загрузить свой авто» или отправьте фото в разделе «🚗 Авто».""";
    }

    public static String wheelSelected(WheelCatalogItem wheel) {
        return """
                Вы выбрали: %s

                Стоимость: %s

                Начать примерку на вашем автомобиле?""".formatted(wheel.getTitle(), wheel.formattedPrice());
    }

    public static String wheelUploaded() {
        return """
                ✅ Диск успешно загружен

                Проверяем изображение…

                ✅ Диск готов к примерке

                Начать генерацию?""";
    }

    public static String fittingReady() {
        return """
                ✅ Примерка готова

                Ваш автомобиль с выбранным диском.

                Результат готов
                Ваш автомобиль с выбранным диском.""";
    }

    public static String validationFailed(String reason) {
        return "❌ Фото не прошло проверку.\n\n" + reason + "\n\nПопробуйте загрузить другое изображение.";
    }

    public static String generationFailed(String reason) {
        return "❌ Не удалось сгенерировать примерку.\n\n" + reason;
    }

    private BotMessages() {
    }
}
