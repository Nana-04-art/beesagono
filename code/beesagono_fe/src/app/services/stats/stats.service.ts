import { Injectable, computed, inject, signal } from '@angular/core';
import { PlayerStats, SeasonStats } from '../../models/stats.model';
import { StorageService } from '../storage/storage.service';
import { CAREER_TIERS, STREAK_MILESTONES } from '../../config/career-tiers.constant';

@Injectable({
    providedIn: 'root'
})
export class StatsService {
    private readonly storage = inject(StorageService);
    private readonly STORAGE_KEY = 'stats';

    // Reactive state
    private readonly _stats = signal<PlayerStats>(this.loadStats());
    readonly stats = this._stats.asReadonly();

    // Derived data
    readonly currentTier = computed(() => this.calculateTier(this._stats()));

    constructor() { }

    // Records the start of a game day without altering the score or rank distribution.
    recordGameStarted(currentDate: string): void {
        this.recordProgress(currentDate, 0, false, null);
    }

    /**
    * Records incremental progress at game completion or when finding a word.
    * @param currentDate Current date YYYY-MM-DD
    * @param dailyScore TOTAL score accumulated today
    * @param isCompletedToday True if today's board has been 100% completed
    * @param dailyRank Name of today's achieved rank (pass null or empty string to skip updating)
    */
    recordProgress(
        currentDate: string,
        dailyScore: number,
        isCompletedToday: boolean,
        dailyRank: string | null
    ): void {
        this._stats.update(currentStats => {
            const stats: PlayerStats = {
                ...currentStats,
                currentSeason: { ...currentStats.currentSeason },
                dailyRankDistribution: { ...currentStats.dailyRankDistribution }
            };

            const todayYear = parseInt(currentDate.split('-')[0], 10);

            // Year Rollover Handling (Season Reset)
            if (stats.currentSeason.year !== todayYear) {
                stats.seasonHistory = {
                    ...stats.seasonHistory,
                    [stats.currentSeason.year]: { ...stats.currentSeason }
                };
                stats.currentSeason = this.createEmptySeason(todayYear);
            }

            // Streak and Days Played Updates
            const isFirstPlayToday = stats.lastPlayedDate !== currentDate;

            if (isFirstPlayToday) {
                stats.gamesPlayed++;

                if (this.isConsecutiveDay(stats.lastPlayedDate, currentDate)) {
                    stats.currentStreak++;
                } else {
                    stats.currentStreak = 1; // First play ever or broken streak
                }

                stats.maxStreak = Math.max(stats.maxStreak, stats.currentStreak);
                stats.lastPlayedDate = currentDate;

                // Reset daily tracking helpers for a new day
                stats.currentSeason._lastRecordedDailyScore = 0;
                stats.currentSeason._isCompletedToday = false;
                stats.currentSeason._lastRecordedRankToday = null;

                // Check and award Milestone Bonuses
                this.checkStreakMilestones(stats.currentStreak, stats.currentSeason);
            }

            // Season Score (Cumulative sum of seasonal points)
            if (dailyScore >= 0) {
                const previousDailyScore = stats.currentSeason._lastRecordedDailyScore || 0;
                const scoreDiff = dailyScore - previousDailyScore;

                if (scoreDiff > 0) {
                    stats.currentSeason.basePointsEarned += scoreDiff;
                    stats.currentSeason._lastRecordedDailyScore = dailyScore;
                }

                stats.currentSeason.totalSeasonPoints =
                    stats.currentSeason.basePointsEarned + stats.currentSeason.bonusStreakPoints;
            }

            // Completion Check (Increments on transition to complete state)
            if (isCompletedToday && !stats.currentSeason._isCompletedToday) {
                stats.gamesCompleted++;
                stats.currentSeason._isCompletedToday = true;
            }

            // Replaces/Updates rank when a higher/new rank is achieved
            if (dailyRank && dailyRank.trim() !== '') {
                const prevRank = stats.currentSeason._lastRecordedRankToday;

                if (prevRank !== dailyRank) {
                    // Decrement counter for previous rank if reached earlier today
                    if (prevRank && stats.dailyRankDistribution[prevRank]) {
                        stats.dailyRankDistribution[prevRank] = Math.max(0, stats.dailyRankDistribution[prevRank] - 1);
                    }

                    // Increment counter for the newly achieved rank
                    stats.dailyRankDistribution[dailyRank] = (stats.dailyRankDistribution[dailyRank] || 0) + 1;
                    stats.currentSeason._lastRecordedRankToday = dailyRank;
                }
            }

            // Update highest tier achieved in the current season
            const calculatedTier = this.calculateTier(stats);
            stats.currentSeason.highestTierAchieved = calculatedTier;

            return stats;
        });

        this.saveStats();
    }

    private checkStreakMilestones(streak: number, season: SeasonStats): void {
        const bonus = STREAK_MILESTONES[streak];
        if (bonus && !season.claimedStreakMilestones.includes(streak)) {
            season.bonusStreakPoints += bonus;
            season.totalSeasonPoints += bonus;
            season.claimedStreakMilestones.push(streak);
        }
    }

    private calculateTier(stats: PlayerStats): string {
        const refDate = stats.lastPlayedDate ? new Date(stats.lastPlayedDate) : new Date();
        const startOfYear = new Date(refDate.getFullYear(), 0, 1);
        const dayOfYear = Math.floor((refDate.getTime() - startOfYear.getTime()) / (1000 * 60 * 60 * 24)) + 1;

        // Estimated maximum achievable points up to this day of the year
        const maxPossibleGlobalPoints = dayOfYear * 25;

        if (maxPossibleGlobalPoints === 0) return CAREER_TIERS[0].name;

        const percentage = (stats.currentSeason.totalSeasonPoints / maxPossibleGlobalPoints) * 100;

        let currentTier = CAREER_TIERS[0].name;
        for (const tier of CAREER_TIERS) {
            if (percentage >= tier.minPercentage) {
                currentTier = tier.name;
            }
        }
        return currentTier;
    }

    private isConsecutiveDay(lastDate: string | null, currentDate: string): boolean {
        if (!lastDate) return false;

        const [lastY, lastM, lastD] = lastDate.split('-').map(Number);
        const [currY, currM, currD] = currentDate.split('-').map(Number);

        const lastUtc = Date.UTC(lastY, lastM - 1, lastD);
        const currUtc = Date.UTC(currY, currM - 1, currD);

        const diffDays = Math.round((currUtc - lastUtc) / (1000 * 60 * 60 * 24));
        return diffDays === 1;
    }

    private loadStats(): PlayerStats {
        const saved = this.storage.load<PlayerStats>(this.STORAGE_KEY);
        const today = new Date().toISOString().split('T')[0];

        let stats: PlayerStats;
        if (saved) {
            stats = saved;
            this.checkStreakContinuity(stats, today);
        } else {
            // Rebuild stats from stored local games if 'stats' key doesn't exist
            stats = this.rebuildStatsFromStorage();
            this.checkStreakContinuity(stats, today);
        }
        return stats;
    }

    private checkStreakContinuity(stats: PlayerStats, today: string): void {
        if (!stats.lastPlayedDate) return;

        const [y, m, d] = today.split('-').map(Number);
        const [lastY, lastM, lastD] = stats.lastPlayedDate.split('-').map(Number);

        const todayUtc = Date.UTC(y, m - 1, d);
        const lastUtc = Date.UTC(lastY, lastM - 1, lastD);

        const diffDays = Math.round((todayUtc - lastUtc) / (1000 * 60 * 60 * 24));

        if (diffDays > 1) {
            stats.currentStreak = 0;
        }
    }

    private rebuildStatsFromStorage(): PlayerStats {
        const newStats: PlayerStats = {
            gamesPlayed: 0,
            gamesCompleted: 0,
            currentStreak: 0,
            maxStreak: 0,
            lastPlayedDate: null,
            currentSeason: this.createEmptySeason(new Date().getFullYear()),
            seasonHistory: {},
            dailyRankDistribution: {}
        };

        const playedDates: string[] = [];

        for (let i = 0; i < localStorage.length; i++) {
            const key = localStorage.key(i);
            if (key?.startsWith('beesagono:game:')) {
                try {
                    const gameData = JSON.parse(localStorage.getItem(key)!);

                    newStats.currentSeason.basePointsEarned += (gameData.score || 0);
                    newStats.gamesPlayed++;

                    if (gameData.isCompleted) {
                        newStats.gamesCompleted++;
                    }

                    const date = key.replace('beesagono:game:', '');
                    playedDates.push(date);
                } catch (e) {
                    console.error('Errore nel parsing della chiave di gioco:', key, e);
                }
            }
        }

        playedDates.sort();

        if (playedDates.length > 0) {
            newStats.lastPlayedDate = playedDates[playedDates.length - 1];
            let streak = 0;

            for (let i = 0; i < playedDates.length; i++) {
                if (i === 0) {
                    streak = 1;
                } else {
                    if (this.isConsecutiveDay(playedDates[i - 1], playedDates[i])) {
                        streak++;
                    } else if (playedDates[i - 1] !== playedDates[i]) {
                        streak = 1;
                    }
                }

                if (streak > newStats.maxStreak) {
                    newStats.maxStreak = streak;
                }
            }

            newStats.currentStreak = streak;
        }

        newStats.currentSeason.totalSeasonPoints = newStats.currentSeason.basePointsEarned;
        return newStats;
    }

    private saveStats(): void {
        this.storage.save<PlayerStats>(this.STORAGE_KEY, this._stats());
    }

    private createEmptySeason(year: number): SeasonStats {
        return {
            year,
            basePointsEarned: 0,
            bonusStreakPoints: 0,
            totalSeasonPoints: 0,
            highestTierAchieved: CAREER_TIERS[0]?.name ?? 'Iniziato',
            claimedStreakMilestones: [],
            _lastRecordedDailyScore: 0,
            _isCompletedToday: false,
            _lastRecordedRankToday: null
        };
    }
}