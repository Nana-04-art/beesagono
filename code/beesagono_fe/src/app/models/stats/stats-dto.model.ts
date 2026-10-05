export interface PlayerStatsResponse {
    userId: string;
    gamesPlayed: number;
    gamesCompleted: number;
    currentStreak: number;
    maxStreak: number;
    longestWordFound: string;
    totalScoreEarned: number;
    averageScorePerGame: number;
    completionRate: number;
}

export interface PlayerSeasonResponse {
    year: number;
    highestTierAchieved: string;
    gamesPlayed: number;
    gamesCompleted: number;
    currentStreak: number;
    maxStreak: number;
    basePoints: number;
    bonusPoints: number;
    totalPoints: number;
}

export interface LeaderboardEntryDto {
    username: string;
    totalPoints: number;
    highestTierAchieved: string;
    rankPosition: number;
}

export interface RankDistributionResponse {
    seasonYear: number;
    totalPlayers: number;
    tierCounts: Record<string, number>;
}