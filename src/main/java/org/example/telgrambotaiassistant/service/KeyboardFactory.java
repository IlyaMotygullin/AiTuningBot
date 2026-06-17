package org.example.telgrambotaiassistant.service;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class KeyboardFactory {

    public ReplyKeyboardMarkup mainMenuKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton(MenuText.BTN_PREMIUM));
        row1.add(new KeyboardButton(MenuText.BTN_AUTO));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton(MenuText.BTN_DISKS));
        row2.add(new KeyboardButton(MenuText.BTN_RESULTS));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton(MenuText.BTN_PROFILE));

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(List.of(row1, row2, row3));
        markup.setResizeKeyboard(true);
        return markup;
    }

    public ReplyKeyboardMarkup welcomeActionsKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton(MenuText.BTN_UPLOAD_CAR));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton(MenuText.BTN_UPLOAD_DISKS));

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(List.of(row1, row2));
        markup.setResizeKeyboard(true);
        return markup;
    }

    public ReplyKeyboardMarkup afterCarSavedKeyboard() {
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton(MenuText.BTN_DISK_LOADING));
        row.add(new KeyboardButton(MenuText.BTN_BACK_TO_CAR));

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(List.of(row));
        markup.setResizeKeyboard(true);
        return markup;
    }

    public ReplyKeyboardMarkup carSectionKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton(MenuText.BTN_UPLOAD_NEW_CAR));
        row1.add(new KeyboardButton(MenuText.BTN_DELETE_CAR));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton(MenuText.BTN_BACK_MAIN));

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(List.of(row1, row2));
        markup.setResizeKeyboard(true);
        return markup;
    }

    public InlineKeyboardMarkup diskChoiceInline() {
        return inlineRows(List.of(oneRow(
                button(MenuText.BTN_CATALOG, CallbackAction.OPEN_CATALOG),
                button(MenuText.BTN_CUSTOM_DISK, CallbackAction.CUSTOM_WHEEL)
        )));
    }

    public InlineKeyboardMarkup catalogInline() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (WheelCatalogItem item : WheelCatalogItem.values()) {
            rows.add(List.of(button(
                    item.getTitle() + " — " + item.formattedPrice(),
                    CallbackAction.PREFIX_CATALOG + item.getId()
            )));
        }
        rows.add(List.of(button("[Показать ещё]", CallbackAction.SHOW_MORE_CATALOG)));
        rows.add(List.of(button("Назад", CallbackAction.BACK_DISK_MENU)));
        return inlineRows(rows);
    }

    public InlineKeyboardMarkup wheelConfirmInline() {
        return inlineRows(List.of(oneRow(
                button("Начать примерку", CallbackAction.START_FITTING),
                button("Назад", CallbackAction.BACK_CATALOG)
        )));
    }

    public InlineKeyboardMarkup customWheelReadyInline() {
        return inlineRows(List.of(oneRow(
                button("⚡ Примерить на авто", CallbackAction.FIT_CUSTOM_WHEEL),
                button("🔄 Загрузить другой диск", CallbackAction.RELOAD_WHEEL)
        )));
    }

    public InlineKeyboardMarkup resultInline() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(oneRow(
                button("💾 Скачать", CallbackAction.RESULT_DOWNLOAD),
                button("📤 Поделиться", CallbackAction.RESULT_SHARE)
        ));
        rows.add(oneRow(button("💿 Попробовать другие диски", CallbackAction.TRY_OTHER_WHEELS)));
        return inlineRows(rows);
    }

    public InlineKeyboardMarkup deleteCarConfirmInline() {
        return inlineRows(List.of(oneRow(
                button("🗑 Удалить авто", CallbackAction.DELETE_CAR),
                button("Назад", CallbackAction.CANCEL_DELETE_CAR)
        )));
    }

    public InlineKeyboardMarkup premiumInline() {
        return inlineRows(List.of(oneRow(
                button("💳 Купить Premium", CallbackAction.BUY_PREMIUM),
                button("Назад", CallbackAction.BACK_PREMIUM)
        )));
    }

    public InlineKeyboardMarkup resultsInline() {
        return inlineRows(List.of(List.of(button("Назад", CallbackAction.BACK_RESULTS))));
    }

    public InlineKeyboardMarkup profileInline() {
        return inlineRows(List.of(oneRow(
                button("📞 Поддержка", CallbackAction.PROFILE_SUPPORT),
                button("Назад", CallbackAction.BACK_PROFILE)
        )));
    }

    private static List<InlineKeyboardButton> oneRow(InlineKeyboardButton... buttons) {
        return Arrays.asList(buttons);
    }

    private InlineKeyboardMarkup inlineRows(List<List<InlineKeyboardButton>> rows) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }
    //
    private static InlineKeyboardButton button(String text, String callbackData) {
        InlineKeyboardButton inlineButton = new InlineKeyboardButton();
        inlineButton.setText(text);
        inlineButton.setCallbackData(callbackData);
        return inlineButton;
    }
}
