package org.example.telgrambotaiassistant.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.telgrambotaiassistant.catalog.WheelCatalogItem;
import org.example.telgrambotaiassistant.generation.FittingRequest;
import org.example.telgrambotaiassistant.generation.FittingResult;
import org.example.telgrambotaiassistant.generation.NanoBananaService;
import org.example.telgrambotaiassistant.keyboard.KeyboardFactory;
import org.example.telgrambotaiassistant.session.BotState;
import org.example.telgrambotaiassistant.session.UserSession;
import org.example.telgrambotaiassistant.session.UserSessionService;
import org.example.telgrambotaiassistant.telegram.TelegramMediaService;
import org.example.telgrambotaiassistant.telegram.TelegramMessageSender;
import org.example.telgrambotaiassistant.text.BotMessages;
import org.example.telgrambotaiassistant.validation.ImageType;
import org.example.telgrambotaiassistant.validation.ImageValidationService;
import org.example.telgrambotaiassistant.validation.ValidationResult;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
public class UpdateHandler {

    private final UserSessionService sessionService;
    private final ImageValidationService imageValidationService;
    private final NanoBananaService nanoBananaService;
    private final TelegramMediaService telegramMediaService;
    private final TelegramMessageSender messageSender;
    private final KeyboardFactory keyboards;
    private final org.example.telgrambotaiassistant.Bot bot;

    public UpdateHandler(
            UserSessionService sessionService,
            ImageValidationService imageValidationService,
            NanoBananaService nanoBananaService,
            TelegramMediaService telegramMediaService,
            TelegramMessageSender messageSender,
            KeyboardFactory keyboards,
            org.example.telgrambotaiassistant.Bot bot
    ) {
        this.sessionService = sessionService;
        this.imageValidationService = imageValidationService;
        this.nanoBananaService = nanoBananaService;
        this.telegramMediaService = telegramMediaService;
        this.messageSender = messageSender;
        this.keyboards = keyboards;
        this.bot = bot;
    }

    public void handle(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
            return;
        }
        if (!update.hasMessage()) {
            return;
        }
        Message message = update.getMessage();
        Long chatId = message.getChatId();

        if (message.hasPhoto() || (message.hasDocument() && message.getDocument().getMimeType() != null
                && message.getDocument().getMimeType().startsWith("image/"))) {
            handlePhoto(chatId, message);
            return;
        }
        if (message.hasText()) {
            handleText(chatId, message.getText().trim());
        }
    }

    private void handleText(long chatId, String text) {
        if (MenuText.CMD_START.equals(text) || MenuText.BTN_MAIN_MENU.equals(text) || MenuText.BTN_BACK_MAIN.equals(text)) {
            showWelcome(chatId);
            return;
        }

        switch (text) {
            case MenuText.BTN_UPLOAD_CAR, MenuText.BTN_UPLOAD_NEW_CAR, MenuText.BTN_BACK_TO_CAR -> requestCarPhoto(chatId);
            case MenuText.BTN_UPLOAD_DISKS, MenuText.BTN_DISK_LOADING, MenuText.BTN_DISKS -> openDiskFlow(chatId);
            case MenuText.BTN_AUTO -> showCarSection(chatId);
            case MenuText.BTN_DELETE_CAR -> messageSender.sendTextWithInline(
                    chatId,
                    "Удалить сохранённое фото автомобиля?",
                    keyboards.deleteCarConfirmInline()
            );
            case MenuText.BTN_PREMIUM -> showPremium(chatId);
            case MenuText.BTN_RESULTS -> showResults(chatId);
            case MenuText.BTN_PROFILE -> showProfile(chatId);
            default -> messageSender.sendText(
                    chatId,
                    "Используйте кнопки меню или отправьте /start",
                    keyboards.mainMenuKeyboard()
            );
        }
    }

    private void handlePhoto(long chatId, Message message) {
        UserSession session = sessionService.getOrCreate(chatId);
        var photoOpt = telegramMediaService.extractLargestPhoto(message);
        if (photoOpt.isEmpty()) {
            messageSender.sendText(chatId, "Отправьте фото в формате JPG, PNG или WEBP.", keyboards.mainMenuKeyboard());
            return;
        }

        BotState state = session.getState();
        ImageType expectedType = resolveExpectedImageType(session, state);

        if (expectedType == ImageType.CAR && session.hasSavedCar() && state != BotState.WAITING_CAR_PHOTO) {
            messageSender.sendText(
                    chatId,
                    "Авто уже сохранено. Для замены нажмите «📸 Загрузить новое авто» в разделе «🚗 Авто».",
                    keyboards.mainMenuKeyboard()
            );
            return;
        }
        if (expectedType == null) {
            messageSender.sendText(
                    chatId,
                    "Сначала выберите действие в меню: загрузка авто или дисков.",
                    keyboards.mainMenuKeyboard()
            );
            return;
        }

        try {
            TelegramMediaService.PhotoPayload photo = photoOpt.get();
            byte[] bytes = telegramMediaService.downloadFile(photo.fileId());

            if (expectedType == ImageType.WHEEL) {
                messageSender.sendText(chatId, "🔍 Проверяем фото…", keyboards.mainMenuKeyboard());
            }

            ValidationResult validation = imageValidationService.validate(bytes, photo.mimeType(), expectedType);
            if (!validation.valid()) {
                messageSender.sendText(chatId, BotMessages.validationFailed(validation.message()), keyboards.mainMenuKeyboard());
                return;
            }

            if (expectedType == ImageType.CAR) {
                boolean hadCar = session.hasSavedCar();
                sessionService.saveCar(chatId, photo.fileId(), bytes);
                messageSender.sendText(chatId, BotMessages.carSaved(hadCar), keyboards.afterCarSavedKeyboard());
                return;
            }

            session.setCustomWheelPhotoFileId(photo.fileId());
            session.setCustomWheelImageBytes(bytes);
            session.setSelectedCatalogWheel(null);
            session.setState(BotState.WHEEL_READY);
            messageSender.sendTextWithInline(chatId, BotMessages.wheelUploaded(), keyboards.customWheelReadyInline());
        } catch (Exception ex) {
            log.error("Ошибка обработки фото chatId={}", chatId, ex);
            messageSender.sendText(chatId, "Не удалось обработать фото. Попробуйте ещё раз.", keyboards.mainMenuKeyboard());
        }
    }

    private void handleCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        String data = callback.getData();
        UserSession session = sessionService.getOrCreate(chatId);

        answerCallback(callback.getId());

        if (CallbackAction.DELETE_CAR.equals(data)) {
            sessionService.deleteCar(chatId);
            messageSender.sendText(chatId, "Автомобиль удалён.", keyboards.mainMenuKeyboard());
            return;
        }
        if (CallbackAction.CANCEL_DELETE_CAR.equals(data)) {
            showCarSection(chatId);
            return;
        }
        if (CallbackAction.BACK_DISK_MENU.equals(data)) {
            openDiskFlow(chatId);
            return;
        }
        if (CallbackAction.OPEN_CATALOG.equals(data) || CallbackAction.BACK_CATALOG.equals(data)) {
            if (!ensureCarOrAsk(chatId, session)) {
                return;
            }
            session.setState(BotState.WHEEL_CATALOG);
            messageSender.sendTextWithInline(chatId, "Доступные диски:", keyboards.catalogInline());
            return;
        }
        if (CallbackAction.SHOW_MORE_CATALOG.equals(data)) {
            messageSender.sendText(chatId, BotMessages.CATALOG_SHOW_MORE, keyboards.catalogInline());
            return;
        }
        if (CallbackAction.CUSTOM_WHEEL.equals(data) || CallbackAction.RELOAD_WHEEL.equals(data)) {
            if (!ensureCarOrAsk(chatId, session)) {
                return;
            }
            session.setState(BotState.WAITING_WHEEL_PHOTO);
            session.clearWheelSelection();
            messageSender.sendText(chatId, BotMessages.WHEEL_UPLOAD_INSTRUCTION, keyboards.mainMenuKeyboard());
            return;
        }
        if (data != null && data.startsWith(CallbackAction.PREFIX_CATALOG)) {
            if (!ensureCarOrAsk(chatId, session)) {
                return;
            }
            String wheelId = data.substring(CallbackAction.PREFIX_CATALOG.length());
            WheelCatalogItem.findById(wheelId).ifPresentOrElse(wheel -> {
                session.setSelectedCatalogWheel(wheel);
                session.setCustomWheelPhotoFileId(null);
                session.setState(BotState.WHEEL_SELECTED);
                messageSender.sendTextWithInline(
                        chatId,
                        BotMessages.wheelSelected(wheel),
                        keyboards.wheelConfirmInline()
                );
            }, () -> messageSender.sendText(chatId, "Диск не найден.", keyboards.mainMenuKeyboard()));
            return;
        }
        if (CallbackAction.START_FITTING.equals(data) || CallbackAction.FIT_CUSTOM_WHEEL.equals(data)) {
            runGeneration(chatId, session);
            return;
        }
        if (CallbackAction.TRY_OTHER_WHEELS.equals(data)) {
            openDiskFlow(chatId);
            return;
        }
        if (CallbackAction.BUY_PREMIUM.equals(data)) {
            messageSender.sendText(chatId, BotMessages.PREMIUM_PAYMENT_STUB, keyboards.premiumInline());
            return;
        }
        if (CallbackAction.BACK_PREMIUM.equals(data) || CallbackAction.BACK_RESULTS.equals(data)
                || CallbackAction.BACK_PROFILE.equals(data)) {
            showWelcome(chatId);
            return;
        }
        if (CallbackAction.PROFILE_SUPPORT.equals(data)) {
            messageSender.sendText(chatId, BotMessages.SUPPORT, keyboards.profileInline());
            return;
        }
        if (CallbackAction.RESULT_DOWNLOAD.equals(data) || CallbackAction.RESULT_SHARE.equals(data)
                || CallbackAction.RESULT_REPEAT.equals(data)) {
            messageSender.sendText(chatId, BotMessages.STUB_ACTION, keyboards.resultInline());
        }
    }

    private void runGeneration(long chatId, UserSession session) {
        if (!session.hasSavedCar()) {
            requestCarPhoto(chatId);
            return;
        }
        boolean hasWheel = session.getCustomWheelPhotoFileId() != null || session.getSelectedCatalogWheel() != null;
        if (!hasWheel) {
            openDiskFlow(chatId);
            return;
        }

        session.setState(BotState.GENERATING);
        messageSender.sendText(chatId, BotMessages.GENERATING, keyboards.mainMenuKeyboard());

        try {
            byte[] carBytes = session.getCarImageBytes();
            if (carBytes == null || carBytes.length == 0) {
                carBytes = telegramMediaService.downloadFile(session.getCarPhotoFileId());
            }

            byte[] wheelBytes = session.getCustomWheelImageBytes();
            if (wheelBytes == null && session.getCustomWheelPhotoFileId() != null) {
                wheelBytes = telegramMediaService.downloadFile(session.getCustomWheelPhotoFileId());
            }

            FittingRequest request = new FittingRequest(
                    carBytes,
                    wheelBytes,
                    session.getSelectedCatalogWheel(),
                    null
            );
            FittingResult result = nanoBananaService.generateFitting(request);
            if (!result.success()) {
                session.setState(BotState.DISK_MENU);
                messageSender.sendText(chatId, BotMessages.generationFailed(result.message()), keyboards.mainMenuKeyboard());
                return;
            }

            session.setState(BotState.RESULT);
            messageSender.sendPhotoBytes(
                    chatId,
                    result.imageBytes(),
                    BotMessages.fittingReady(),
                    keyboards.resultInline()
            );
        } catch (Exception ex) {
            log.error("Ошибка генерации chatId={}", chatId, ex);
            session.setState(BotState.DISK_MENU);
            messageSender.sendText(chatId, BotMessages.generationFailed("Попробуйте позже."), keyboards.mainMenuKeyboard());
        }
    }

    private void showWelcome(long chatId) {
        UserSession session = sessionService.getOrCreate(chatId);
        session.setState(BotState.MAIN_MENU);
        messageSender.sendText(chatId, BotMessages.WELCOME, keyboards.welcomeActionsKeyboard());
        messageSender.sendText(chatId, "Нижнее меню:", keyboards.mainMenuKeyboard());
    }

    private void requestCarPhoto(long chatId) {
        UserSession session = sessionService.getOrCreate(chatId);
        session.setState(BotState.WAITING_CAR_PHOTO);
        messageSender.sendText(chatId, BotMessages.CAR_UPLOAD_INSTRUCTION, keyboards.mainMenuKeyboard());
    }

    private void showCarSection(long chatId) {
        UserSession session = sessionService.getOrCreate(chatId);
        session.setState(session.hasSavedCar() ? BotState.DISK_MENU : BotState.MAIN_MENU);
        messageSender.sendText(chatId, BotMessages.carMenu(session.hasSavedCar()), keyboards.carSectionKeyboard());
    }

    private void openDiskFlow(long chatId) {
        UserSession session = sessionService.getOrCreate(chatId);
        if (!session.hasSavedCar()) {
            session.setState(BotState.WAITING_CAR_PHOTO);
            messageSender.sendText(chatId, BotMessages.diskMenuNeedCar(), keyboards.welcomeActionsKeyboard());
            return;
        }
        session.setState(BotState.DISK_MENU);
        messageSender.sendTextWithInline(chatId, BotMessages.diskMenuWithSavedCar(), keyboards.diskChoiceInline());
    }

    private void showPremium(long chatId) {
        sessionService.getOrCreate(chatId).setState(BotState.MAIN_MENU);
        messageSender.sendTextWithInline(chatId, BotMessages.PREMIUM_INACTIVE, keyboards.premiumInline());
    }

    private void showResults(long chatId) {
        sessionService.getOrCreate(chatId).setState(BotState.MAIN_MENU);
        messageSender.sendText(chatId, BotMessages.RESULTS_HEADER, keyboards.mainMenuKeyboard());
        messageSender.sendText(chatId, BotMessages.RESULTS_EMPTY, keyboards.resultsInline());
    }

    private void showProfile(long chatId) {
        sessionService.getOrCreate(chatId).setState(BotState.MAIN_MENU);
        messageSender.sendTextWithInline(chatId, BotMessages.PROFILE, keyboards.profileInline());
    }

    private boolean ensureCarOrAsk(long chatId, UserSession session) {
        if (session.hasSavedCar()) {
            return true;
        }
        messageSender.sendText(chatId, BotMessages.diskMenuNeedCar(), keyboards.welcomeActionsKeyboard());
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

    private void answerCallback(String callbackId) {
        try {
            bot.execute(AnswerCallbackQuery.builder().callbackQueryId(callbackId).build());
        } catch (TelegramApiException e) {
            log.warn("Не удалось ответить на callback: {}", e.getMessage());
        }
    }
}
