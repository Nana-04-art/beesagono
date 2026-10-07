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

/**
 * Response returned by the GET and POST /api/stats/me endpoints
 */
export interface PlayerStatsResponse {
    userId: string;
    gamesPlayed: number;
    gamesCompleted: number;
    currentStreak: number;
    maxStreak: number;
    longestWordFound?: string | null;
    totalScoreEarned: number;
    averageScorePerGame: number;
    completionRate: number;
}

/**
 * Complete local model that extends/integrates application data
 */
export interface PlayerStats {
    userId?: string;
    gamesPlayed: number;
    gamesCompleted: number;
    currentStreak: number;
    maxStreak: number;
    lastPlayedDate: string | null;
    longestWordFound?: string | null;
    totalScoreEarned?: number;
    averageScorePerGame?: number;
    completionRate?: number;
    currentSeason?: SeasonStats;
    seasonHistory?: Record<number, SeasonStats>;
    dailyRankDistribution?: Record<string, number>;
}