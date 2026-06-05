package org.example.telgrambotaiassistant.bot;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImageStorage {
    final Map<Long, List<byte[]>> storage = new HashMap<>();

    public void addImgToStorage(Long idChat, byte[] imgUser) {
        storage
                .computeIfAbsent(idChat, k -> new ArrayList<>())
                .add(imgUser);
    }

    public List<byte[]> get(Long idChat) {
        return storage.getOrDefault(idChat, new ArrayList<>());
    }

    public void clear(Long idChat) {
        storage.remove(idChat);
    }
}
