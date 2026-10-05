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
    _lastRecordedDailyScore?: number;
    _isCompletedToday?: boolean;
    _lastRecordedRankToday?: string | null;
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