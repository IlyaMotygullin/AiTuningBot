package org.example.telgrambotaiassistant.service;

import org.example.telgrambotaiassistant.bot.BotState;
import org.example.telgrambotaiassistant.bot.UserSession;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserSessionService {

    private final Map<Long, UserSession> sessions = new ConcurrentHashMap<>();

    public UserSession getOrCreate(long chatId) {
        return sessions.computeIfAbsent(chatId, UserSession::new);
    }

    public void saveCar(long chatId, String fileId, byte[] imageBytes) {
        UserSession session = getOrCreate(chatId);
        session.setCarPhotoFileId(fileId);
        session.setCarImageBytes(imageBytes);
        session.setCarUploadedAt(LocalDateTime.now());
        session.clearWheelSelection();
        session.setState(BotState.DISK_MENU);
    }

    public void deleteCar(long chatId) {
        UserSession session = getOrCreate(chatId);
        session.clearCar();
        session.clearWheelSelection();
        session.setState(BotState.MAIN_MENU);
    }
}
