export interface CalculateWordScoreRequest {
    word: string;
    isMielegramma: boolean;
}

export interface CalculateRankRequest {
    currentScore: number;
    maxScore: number;
}

export interface CalculateCareerTierRequest {
    totalSeasonalPoints: number;
    annualTargetPoints: number;
}