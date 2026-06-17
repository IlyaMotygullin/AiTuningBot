package org.example.telgrambotaiassistant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telgrambotaiassistant.bot.Bot;
import org.example.telgrambotaiassistant.bot.BotState;
import org.example.telgrambotaiassistant.bot.UserSession;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotFlowService {

    private final UserSessionService sessionService;
    private final ImageValidationService imageValidationService;
    private final GenerateImageService generateImageService;
    private final TelegramService telegramService;
    private final KeyboardFactory keyboards;

    public void handle(Bot bot, Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallback(bot, update.getCallbackQuery());
                return;
            }
            if (!update.hasMessage()) {
                return;
            }
            Message message = update.getMessage();
            Long chatId = message.getChatId();

            if (message.hasPhoto() || (message.hasDocument() && message.getDocument().getMimeType() != null
                    && message.getDocument().getMimeType().startsWith("image/"))) {
                handlePhoto(bot, chatId, message);
                return;
            }
            if (message.hasText()) {
                handleText(bot, chatId, message.getText().trim());
            }
        } catch (Exception ex) {
            log.error("Ошибка обработки update", ex);
        }
    }

    private void handleText(Bot bot, long chatId, String text) {
        if (MenuText.CMD_START.equals(text) || MenuText.BTN_MAIN_MENU.equals(text) || MenuText.BTN_BACK_MAIN.equals(text)) {
            showWelcome(bot, chatId);
            return;
        }

        switch (text) {
            case MenuText.BTN_UPLOAD_CAR, MenuText.BTN_UPLOAD_NEW_CAR, MenuText.BTN_BACK_TO_CAR -> requestCarPhoto(bot, chatId);
            case MenuText.BTN_UPLOAD_DISKS, MenuText.BTN_DISK_LOADING, MenuText.BTN_DISKS -> openDiskFlow(bot, chatId);
            case MenuText.BTN_AUTO -> showCarSection(bot, chatId);
            case MenuText.BTN_DELETE_CAR -> telegramService.sendTextWithInline(
                    bot, chatId, "Удалить сохранённое фото автомобиля?", keyboards.deleteCarConfirmInline());
            case MenuText.BTN_PREMIUM -> showPremium(bot, chatId);
            case MenuText.BTN_RESULTS -> showResults(bot, chatId);
            case MenuText.BTN_PROFILE -> showProfile(bot, chatId);
            default -> telegramService.sendText(
                    bot, chatId, "Используйте кнопки меню или отправьте /start", keyboards.mainMenuKeyboard());
        }
    }

    private void handlePhoto(Bot bot, long chatId, Message message) {
        UserSession session = sessionService.getOrCreate(chatId);
        var photoOpt = telegramService.extractLargestPhoto(message);
        if (photoOpt.isEmpty()) {
            telegramService.sendText(bot, chatId, "Отправьте фото в формате JPG, PNG или WEBP.", keyboards.mainMenuKeyboard());
            return;
        }

        BotState state = session.getState();
        ImageType expectedType = resolveExpectedImageType(session, state);

        if (expectedType == ImageType.CAR && session.hasSavedCar() && state != BotState.WAITING_CAR_PHOTO) {
            telegramService.sendText(bot, chatId,
                    "Авто уже сохранено. Для замены нажмите «📸 Загрузить новое авто» в разделе «🚗 Авто».",
                    keyboards.mainMenuKeyboard());
            return;
        }
        if (expectedType == null) {
            telegramService.sendText(bot, chatId,
                    "Сначала выберите действие в меню: загрузка авто или дисков.",
                    keyboards.mainMenuKeyboard());
            return;
        }

        try {
            TelegramService.PhotoPayload photo = photoOpt.get();
            byte[] bytes = telegramService.downloadFile(bot, photo.fileId());

            if (expectedType == ImageType.WHEEL) {
                telegramService.sendText(bot, chatId, "🔍 Проверяем фото…", keyboards.mainMenuKeyboard());
            }

            ValidationResult validation = imageValidationService.validate(bytes, photo.mimeType(), expectedType);
            if (!validation.valid()) {
                telegramService.sendText(bot, chatId, BotMessages.validationFailed(validation.message()), keyboards.mainMenuKeyboard());
                return;
            }

            if (expectedType == ImageType.CAR) {
                boolean hadCar = session.hasSavedCar();
                sessionService.saveCar(chatId, photo.fileId(), bytes);
                telegramService.sendText(bot, chatId, BotMessages.carSaved(hadCar), keyboards.afterCarSavedKeyboard());
                return;
            }

            session.setCustomWheelPhotoFileId(photo.fileId());
            session.setCustomWheelImageBytes(bytes);
            session.setSelectedCatalogWheel(null);
            session.setState(BotState.WHEEL_READY);
            telegramService.sendTextWithInline(bot, chatId, BotMessages.wheelUploaded(), keyboards.customWheelReadyInline());
        } catch (Exception ex) {
            log.error("Ошибка обработки фото chatId={}", chatId, ex);
            telegramService.sendText(bot, chatId, "Не удалось обработать фото. Попробуйте ещё раз.", keyboards.mainMenuKeyboard());
        }
    }

    private void handleCallback(Bot bot, CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        String data = callback.getData();
        UserSession session = sessionService.getOrCreate(chatId);

        answerCallback(bot, callback.getId());

        if (CallbackAction.DELETE_CAR.equals(data)) {
            sessionService.deleteCar(chatId);
            telegramService.sendText(bot, chatId, "Автомобиль удалён.", keyboards.mainMenuKeyboard());
            return;
        }
        if (CallbackAction.CANCEL_DELETE_CAR.equals(data)) {
            showCarSection(bot, chatId);
            return;
        }
        if (CallbackAction.BACK_DISK_MENU.equals(data)) {
            openDiskFlow(bot, chatId);
            return;
        }
        if (CallbackAction.OPEN_CATALOG.equals(data) || CallbackAction.BACK_CATALOG.equals(data)) {
            if (!ensureCarOrAsk(bot, chatId, session)) {
                return;
            }
            session.setState(BotState.WHEEL_CATALOG);
            telegramService.sendTextWithInline(bot, chatId, "Доступные диски:", keyboards.catalogInline());
            return;
        }
        if (CallbackAction.SHOW_MORE_CATALOG.equals(data)) {
            telegramService.sendText(bot, chatId, BotMessages.CATALOG_SHOW_MORE, keyboards.catalogInline());
            return;
        }
        if (CallbackAction.CUSTOM_WHEEL.equals(data) || CallbackAction.RELOAD_WHEEL.equals(data)) {
            if (!ensureCarOrAsk(bot, chatId, session)) {
                return;
            }
            session.setState(BotState.WAITING_WHEEL_PHOTO);
            session.clearWheelSelection();
            telegramService.sendText(bot, chatId, BotMessages.WHEEL_UPLOAD_INSTRUCTION, keyboards.mainMenuKeyboard());
            return;
        }
        if (data != null && data.startsWith(CallbackAction.PREFIX_CATALOG)) {
            if (!ensureCarOrAsk(bot, chatId, session)) {
                return;
            }
            String wheelId = data.substring(CallbackAction.PREFIX_CATALOG.length());
            WheelCatalogItem.findById(wheelId).ifPresentOrElse(wheel -> {
                session.setSelectedCatalogWheel(wheel);
                session.setCustomWheelPhotoFileId(null);
                session.setState(BotState.WHEEL_SELECTED);
                telegramService.sendTextWithInline(bot, chatId, BotMessages.wheelSelected(wheel), keyboards.wheelConfirmInline());
            }, () -> telegramService.sendText(bot, chatId, "Диск не найден.", keyboards.mainMenuKeyboard()));
            return;
        }
        if (CallbackAction.START_FITTING.equals(data) || CallbackAction.FIT_CUSTOM_WHEEL.equals(data)) {
            runGeneration(bot, chatId, session);
            return;
        }
        if (CallbackAction.TRY_OTHER_WHEELS.equals(data)) {
            openDiskFlow(bot, chatId);
            return;
        }
        if (CallbackAction.BUY_PREMIUM.equals(data)) {
            telegramService.sendText(bot, chatId, BotMessages.PREMIUM_PAYMENT_STUB, keyboards.premiumInline());
            return;
        }
        if (CallbackAction.BACK_PREMIUM.equals(data) || CallbackAction.BACK_RESULTS.equals(data)
                || CallbackAction.BACK_PROFILE.equals(data)) {
            showWelcome(bot, chatId);
            return;
        }
        if (CallbackAction.PROFILE_SUPPORT.equals(data)) {
            telegramService.sendText(bot, chatId, BotMessages.SUPPORT, keyboards.profileInline());
            return;
        }
        if (CallbackAction.RESULT_DOWNLOAD.equals(data) || CallbackAction.RESULT_SHARE.equals(data)
                || CallbackAction.RESULT_REPEAT.equals(data)) {
            telegramService.sendText(bot, chatId, BotMessages.STUB_ACTION, keyboards.resultInline());
        }
    }

    private void runGeneration(Bot bot, long chatId, UserSession session) {
        if (!session.hasSavedCar()) {
            requestCarPhoto(bot, chatId);
            return;
        }
        boolean hasWheel = session.getCustomWheelPhotoFileId() != null || session.getSelectedCatalogWheel() != null;
        if (!hasWheel) {
            openDiskFlow(bot, chatId);
            return;
        }

        session.setState(BotState.GENERATING);
        telegramService.sendText(bot, chatId, BotMessages.GENERATING, keyboards.mainMenuKeyboard());

        try {
            byte[] carBytes = session.getCarImageBytes();
            if (carBytes == null || carBytes.length == 0) {
                carBytes = telegramService.downloadFile(bot, session.getCarPhotoFileId());
            }

            byte[] wheelBytes = session.getCustomWheelImageBytes();
            if (wheelBytes == null && session.getCustomWheelPhotoFileId() != null) {
                wheelBytes = telegramService.downloadFile(bot, session.getCustomWheelPhotoFileId());
            }

            FittingRequest request = new FittingRequest(carBytes, wheelBytes, session.getSelectedCatalogWheel(), null);
            FittingResult result = generateImageService.generateFitting(request);
            if (!result.success()) {
                session.setState(BotState.DISK_MENU);
                telegramService.sendText(bot, chatId, BotMessages.generationFailed(result.message()), keyboards.mainMenuKeyboard());
                return;
            }

            session.setState(BotState.RESULT);
            telegramService.sendPhotoBytes(bot, chatId, result.imageBytes(), BotMessages.fittingReady(), keyboards.resultInline());
        } catch (Exception ex) {
            log.error("Ошибка генерации chatId={}", chatId, ex);
            session.setState(BotState.DISK_MENU);
            telegramService.sendText(bot, chatId, BotMessages.generationFailed("Попробуйте позже."), keyboards.mainMenuKeyboard());
        }
    }

    private void showWelcome(Bot bot, long chatId) {
        UserSession session = sessionService.getOrCreate(chatId);
        session.setState(BotState.MAIN_MENU);
        telegramService.sendText(bot, chatId, BotMessages.WELCOME, keyboards.welcomeActionsKeyboard());
        telegramService.sendText(bot, chatId, "Нижнее меню:", keyboards.mainMenuKeyboard());
    }

    private void requestCarPhoto(Bot bot, long chatId) {
        UserSession session = sessionService.getOrCreate(chatId);
        session.setState(BotState.WAITING_CAR_PHOTO);
        telegramService.sendText(bot, chatId, BotMessages.CAR_UPLOAD_INSTRUCTION, keyboards.mainMenuKeyboard());
    }

    private void showCarSection(Bot bot, long chatId) {
        UserSession session = sessionService.getOrCreate(chatId);
        session.setState(session.hasSavedCar() ? BotState.DISK_MENU : BotState.MAIN_MENU);
        telegramService.sendText(bot, chatId, BotMessages.carMenu(session.hasSavedCar()), keyboards.carSectionKeyboard());
    }

    private void openDiskFlow(Bot bot, long chatId) {
        UserSession session = sessionService.getOrCreate(chatId);
        if (!session.hasSavedCar()) {
            session.setState(BotState.WAITING_CAR_PHOTO);
            telegramService.sendText(bot, chatId, BotMessages.diskMenuNeedCar(), keyboards.welcomeActionsKeyboard());
            return;
        }
        session.setState(BotState.DISK_MENU);
        telegramService.sendTextWithInline(bot, chatId, BotMessages.diskMenuWithSavedCar(), keyboards.diskChoiceInline());
    }

    private void showPremium(Bot bot, long chatId) {
        sessionService.getOrCreate(chatId).setState(BotState.MAIN_MENU);
        telegramService.sendTextWithInline(bot, chatId, BotMessages.PREMIUM_INACTIVE, keyboards.premiumInline());
    }

    private void showResults(Bot bot, long chatId) {
        sessionService.getOrCreate(chatId).setState(BotState.MAIN_MENU);
        telegramService.sendText(bot, chatId, BotMessages.RESULTS_HEADER, keyboards.mainMenuKeyboard());
        telegramService.sendText(bot, chatId, BotMessages.RESULTS_EMPTY, keyboards.resultsInline());
    }

    private void showProfile(Bot bot, long chatId) {
        sessionService.getOrCreate(chatId).setState(BotState.MAIN_MENU);
        telegramService.sendTextWithInline(bot, chatId, BotMessages.PROFILE, keyboards.profileInline());
    }

    private boolean ensureCarOrAsk(Bot bot, long chatId, UserSession session) {
        if (session.hasSavedCar()) {
            return true;
        }
        telegramService.sendText(bot, chatId, BotMessages.diskMenuNeedCar(), keyboards.welcomeActionsKeyboard());
        return false;
    }

    private ImageType resolveExpectedImageType(UserSession session, BotState state) {
        return switch (state) {
            case WAITING_CAR_PHOTO -> ImageType.CAR;
            case WAITING_WHEEL_PHOTO, DISK_MENU, WHEEL_CATALOG, WHEEL_SELECTED, WHEEL_READY -> ImageType.WHEEL;
            case MAIN_MENU, RESULT, GENERATING -> session.hasSavedCar() ? null : ImageType.CAR;
            default -> null;
        };
    }

    private void answerCallback(Bot bot, String callbackId) {
        try {
            bot.execute(AnswerCallbackQuery.builder().callbackQueryId(callbackId).build());
        } catch (TelegramApiException e) {
            log.warn("Не удалось ответить на callback: {}", e.getMessage());
        }
    }
}
