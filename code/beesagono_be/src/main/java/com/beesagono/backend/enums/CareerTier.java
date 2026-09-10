package com.beesagono.backend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Comparator;

@Getter
@RequiredArgsConstructor
public enum CareerTier {
    EGG(0, "🥚 Uovo d'Ape"),
    LARVA(15, "🐛 Larva"),
    NURSE_BEE(30, "🍼 Ape Nutrice"),
    WORKER_BEE(45, "🛠️ Ape Operaia"),
    FORAGER_BEE(60, "🌸 Ape Bottinatrice"),
    GUARDIAN_BEE(75, "🗝️ Ape Custode"),
    DEFENDER_BEE(85, "🛡️ Ape Guardiana"),
    ARCHITECT_BEE(95, "📐 Ape Architetto"),
    SEASON_QUEEN(100, "👑 Ape Regina della Stagione");

    private final int minPercentage;
    private final String name;

    public static CareerTier getTierForPercentage(double percentage) {
        return Arrays.stream(values())
                .sorted(Comparator.comparingInt(CareerTier::getMinPercentage).reversed())
                .filter(tier -> percentage >= tier.getMinPercentage())
                .findFirst()
                .orElse(EGG);
    }
}