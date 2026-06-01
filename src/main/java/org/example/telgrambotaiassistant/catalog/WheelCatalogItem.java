package org.example.telgrambotaiassistant.catalog;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum WheelCatalogItem {

    BBS_LM("bbs_lm", "BBS LM", 18_990),
    RAYS_TE37("rays_te37", "RAYS TE37", 19_990),
    VOSSEN_HF5("vossen_hf5", "VOSSEN HF-5", 23_990),
    WORK_VS_XV("work_vs_xv", "WORK VS XV", 21_990);

    private final String id;
    private final String title;
    private final int priceRub;

    public static Optional<WheelCatalogItem> findById(String id) {
        return Arrays.stream(values())
                .filter(item -> item.id.equals(id))
                .findFirst();
    }

    public String formattedPrice() {
        return String.format("%,d ₽", priceRub).replace(',', ' ');
    }
}
