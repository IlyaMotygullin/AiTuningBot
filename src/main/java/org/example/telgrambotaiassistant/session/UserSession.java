package org.example.telgrambotaiassistant.session;

import lombok.Data;
import org.example.telgrambotaiassistant.catalog.WheelCatalogItem;

import java.time.LocalDateTime;

@Data
public class UserSession {

    private final long chatId;
    private BotState state = BotState.MAIN_MENU;
    private String carPhotoFileId;
    private byte[] carImageBytes;
    private LocalDateTime carUploadedAt;
    private String customWheelPhotoFileId;
    private byte[] customWheelImageBytes;
    private WheelCatalogItem selectedCatalogWheel;
    private String lastResultPhotoFileId;

    public UserSession(long chatId) {
        this.chatId = chatId;
    }

    public boolean hasSavedCar() {
        return carPhotoFileId != null && !carPhotoFileId.isBlank();
    }

    public void clearCar() {
        carPhotoFileId = null;
        carImageBytes = null;
        carUploadedAt = null;
    }

    public void clearWheelSelection() {
        customWheelPhotoFileId = null;
        customWheelImageBytes = null;
        selectedCatalogWheel = null;
    }
}
