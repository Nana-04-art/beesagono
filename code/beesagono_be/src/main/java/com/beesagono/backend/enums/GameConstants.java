package com.beesagono.backend.enums;

import java.util.Map;

public final class GameConstants {

    private GameConstants() {
    }

    public static final Map<Integer, Integer> STREAK_MILESTONES = Map.of(
            3, 50,
            7, 150,
            15, 350,
            30, 800,
            50, 1500,
            100, 3500,
            200, 8000,
            365, 20000);
}