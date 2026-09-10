package com.beesagono.backend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Comparator;

@Getter
@RequiredArgsConstructor
public enum RankTier {
    INITIAL(0, "🌱 Iniziato"),
    FRESH_MIND(2, "🍃 Mente Fresca"),
    BEGINNER(5, "🐣 Principiante"),
    ADVANCED(8, "🚀 Avanzato"),
    EXPERT(15, "💡 Esperto"),
    EXCELLENT(25, "⭐ Eccellente"),
    GENIUS(40, "🧠 Genio"),
    MASTER(70, "👑 Maestro"),
    QUEEN_BEE(100, "🐝 Ape Regina");

    private final int thresholdPercentage;
    private final String label;

    public static RankTier getRankForPercentage(double percentage) {
        return Arrays.stream(values())
                .sorted(Comparator.comparingInt(RankTier::getThresholdPercentage).reversed())
                .filter(tier -> percentage >= tier.getThresholdPercentage())
                .findFirst()
                .orElse(INITIAL);
    }
}