export interface CareerTier {
    name: string;
    minPercentage: number;
}

export interface SeasonStats {
    year: number;
    basePointsEarned: number;
    bonusStreakPoints: number;
    totalSeasonPoints: number;
    highestTierAchieved: string;
    claimedStreakMilestones: number[];
    _lastRecordedDailyScore?: number
}

export interface PlayerStats {
    gamesPlayed: number;
    gamesCompleted: number;
    currentStreak: number;
    maxStreak: number;
    lastPlayedDate: string | null;
    currentSeason: SeasonStats;
    seasonHistory: Record<number, SeasonStats>;
    dailyRankDistribution: Record<string, number>;
}

export interface GameStats {
    gamesPlayed: number;
    gamesWon: number; // Matches in which the maximum rank or a target threshold was reached
    currentStreak: number;
    maxStreak: number;
    lastPlayedDate: string; // YYYY-MM-DD format
    rankDistribution: Record<string, number>; // Es: { 'Iniziato': 2, 'Genio': 5 }
}