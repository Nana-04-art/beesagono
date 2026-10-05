export interface BadgeResponse {
    code: string;
    title: string;
    description: string;
    iconUrl: string;
    category: string;
    unlocked: boolean;
    unlockedAt: string | null; // ISO Instant string da Backend Java
    currentProgress: number;
    targetValue: number;
    progressPercentage: number;
}

export interface UserBadgesSummaryResponse {
    totalBadges: number;
    unlockedBadges: number;
    overallCompletionPercentage: number;
    badges: BadgeResponse[];
}