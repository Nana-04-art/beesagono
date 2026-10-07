import { Injectable, computed, inject, signal } from '@angular/core';
import { PlayerStats, SeasonStats } from '../../models/stats/stats.model';
import { StorageService } from '../storage/storage.service';
import { CAREER_TIERS, STREAK_MILESTONES } from '../../config/career-tiers.constant';
import { getTodayIsoString } from '../../utils/game-input.utils';
import { isValidGameState, isValidIsoDate } from '../../utils/game-state.validator';

@Injectable({
    providedIn: 'root'
})
export class StatsService {
    private readonly storage = inject(StorageService);
    private readonly STORAGE_KEY = 'stats';
    private readonly GAME_KEY_PREFIX = 'beesagono:game:';

    // Reactive state
    private readonly _stats = signal<PlayerStats>(this.loadStats());
    readonly stats = this._stats.asReadonly();

    // Derived data
    readonly currentTier = computed(() => this.calculateTier(this._stats()));

    public syncWithRemoteStats(remoteStats: any): void {
        if (!remoteStats) return;

        const today = getTodayIsoString(new Date());

        // Normalize payload to ensure nested structures and arrays exist
        const normalized = this.normalizeStatsPayload(remoteStats);

        // Verify streak continuity relative to today's date
        this.checkStreakContinuity(normalized, today);

        // Recalculate updated career tier
        if (normalized.currentSeason) {
            normalized.currentSeason.highestTierAchieved = this.calculateTier(normalized);
        }

        // Update local Signal and persist snapshot to LocalStorage
        this._stats.set(normalized);
        this.saveStats(normalized);
    }

    /**
     * Records the start of a game day without altering the score or rank distribution.
     */
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
            const currentSeason = currentStats.currentSeason ?? this.createEmptySeason(new Date().getFullYear());
            const seasonHistory = currentStats.seasonHistory ?? {};
            const dailyRankDistribution = currentStats.dailyRankDistribution ?? {};

            // Deep clone of nested objects and arrays to protect state immutability
            const stats: PlayerStats = {
                ...currentStats,
                currentSeason: {
                    ...currentSeason,
                    claimedStreakMilestones: [...(currentSeason.claimedStreakMilestones || [])]
                },
                dailyRankDistribution: { ...dailyRankDistribution },
                seasonHistory: Object.keys(seasonHistory).reduce((acc, year) => {
                    const yr = Number(year);
                    const histSeason = seasonHistory[yr];
                    if (histSeason) {
                        acc[yr] = {
                            ...histSeason,
                            claimedStreakMilestones: [...(histSeason.claimedStreakMilestones || [])]
                        };
                    }
                    return acc;
                }, {} as Record<number, SeasonStats>)
            };

            const todayYear = parseInt(currentDate.split('-')[0], 10);

            // Year Rollover Handling (Season Reset)
            if (stats.currentSeason && stats.currentSeason.year !== todayYear) {
                stats.seasonHistory = {
                    ...(stats.seasonHistory ?? {}),
                    [stats.currentSeason.year]: {
                        ...stats.currentSeason,
                        claimedStreakMilestones: [...stats.currentSeason.claimedStreakMilestones]
                    }
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
                    stats.currentStreak = 1;
                }

                stats.maxStreak = Math.max(stats.maxStreak, stats.currentStreak);
                stats.lastPlayedDate = currentDate;

                if (stats.currentSeason) {
                    stats.currentSeason._lastRecordedDailyScore = 0;
                    stats.currentSeason._isCompletedToday = false;
                    stats.currentSeason._lastRecordedRankToday = null;
                    // Milestone bonus assignment
                    this.checkStreakMilestones(stats.currentStreak, stats.currentSeason);
                }
            }

            if (dailyScore >= 0 && stats.currentSeason) {
                const previousDailyScore = stats.currentSeason._lastRecordedDailyScore || 0;
                const scoreDiff = dailyScore - previousDailyScore;

                if (scoreDiff > 0) {
                    stats.currentSeason.basePointsEarned += scoreDiff;
                    stats.currentSeason._lastRecordedDailyScore = dailyScore;
                }

                stats.currentSeason.totalSeasonPoints =
                    stats.currentSeason.basePointsEarned + stats.currentSeason.bonusStreakPoints;
            }

            if (isCompletedToday && stats.currentSeason && !stats.currentSeason._isCompletedToday) {
                stats.gamesCompleted++;
                stats.currentSeason._isCompletedToday = true;
            }

            if (dailyRank && dailyRank.trim() !== '' && stats.currentSeason) {
                const prevRank = stats.currentSeason._lastRecordedRankToday;

                if (prevRank !== dailyRank) {
                    if (prevRank && stats.dailyRankDistribution && stats.dailyRankDistribution[prevRank]) {
                        stats.dailyRankDistribution[prevRank] = Math.max(0, stats.dailyRankDistribution[prevRank] - 1);
                    }

                    if (!stats.dailyRankDistribution) {
                        stats.dailyRankDistribution = {};
                    }
                    stats.dailyRankDistribution[dailyRank] = (stats.dailyRankDistribution[dailyRank] || 0) + 1;
                    stats.currentSeason._lastRecordedRankToday = dailyRank;
                }
            }

            if (stats.currentSeason) {
                stats.currentSeason.highestTierAchieved = this.calculateTier(stats);
            }

            return stats;
        });

        this.saveStats();
    }

    private checkStreakMilestones(streak: number, season: SeasonStats): void {
        const bonus = STREAK_MILESTONES[streak];
        if (bonus && season.claimedStreakMilestones && !season.claimedStreakMilestones.includes(streak)) {
            season.bonusStreakPoints += bonus;
            season.totalSeasonPoints += bonus;
            season.claimedStreakMilestones.push(streak);
        }
    }

    private calculateTier(stats: PlayerStats): string {
        const ANNUAL_TARGET_POINTS = 80000;
        const totalPoints = stats.currentSeason?.totalSeasonPoints ?? 0;
        
        // Percentage calculation based on the backend's annual target
        const percentage = (totalPoints / ANNUAL_TARGET_POINTS) * 100;

        let currentTier = CAREER_TIERS[0].name;
        for (const tier of CAREER_TIERS) {
            if (percentage >= tier.minPercentage) {
                currentTier = tier.name;
            }
        }
        return currentTier;
    }

    private isConsecutiveDay(lastDate: string | null, currentDate: string): boolean {
        if (!lastDate || !isValidIsoDate(lastDate) || !isValidIsoDate(currentDate)) {
            return false;
        }

        const [lastY, lastM, lastD] = lastDate.split('-').map(Number);
        const [currY, currM, currD] = currentDate.split('-').map(Number);

        const lastUtc = Date.UTC(lastY, lastM - 1, lastD);
        const currUtc = Date.UTC(currY, currM - 1, currD);

        const diffDays = Math.round((currUtc - lastUtc) / (1000 * 60 * 60 * 24));
        return diffDays === 1;
    }

    private loadStats(): PlayerStats {
        const saved = this.storage.load<PlayerStats>(this.STORAGE_KEY);
        const today = getTodayIsoString(new Date());

        let stats: PlayerStats;

        if (saved && typeof saved === 'object') {
            stats = this.normalizeStatsPayload(saved);
            this.checkStreakContinuity(stats, today);
        } else {
            // If stats are rebuilt from storage GameStates, save the result immediately
            stats = this.rebuildStatsFromStorage();
            this.checkStreakContinuity(stats, today);
            this.saveStats(stats);
        }
        return stats;
    }

    private normalizeStatsPayload(payload: any): PlayerStats {
        const currentYear = new Date().getFullYear();
        const currentSeasonRaw = payload.currentSeason ?? {};

        // Map both standard frontend properties and backend fields (basePoints, bonusPoints, totalPoints)
        const normalizedSeason: SeasonStats = {
            year: currentSeasonRaw.year ?? currentYear,
            basePointsEarned: currentSeasonRaw.basePointsEarned ?? currentSeasonRaw.basePoints ?? 0,
            bonusStreakPoints: currentSeasonRaw.bonusStreakPoints ?? currentSeasonRaw.bonusPoints ?? 0,
            totalSeasonPoints: currentSeasonRaw.totalSeasonPoints ?? currentSeasonRaw.totalPoints ?? 0,
            highestTierAchieved: currentSeasonRaw.highestTierAchieved ?? CAREER_TIERS[0]?.name ?? 'Initiated',
            claimedStreakMilestones: Array.isArray(currentSeasonRaw.claimedStreakMilestones)
                ? [...currentSeasonRaw.claimedStreakMilestones]
                : [],
            _lastRecordedDailyScore: currentSeasonRaw._lastRecordedDailyScore ?? 0,
            _isCompletedToday: currentSeasonRaw._isCompletedToday ?? false,
            _lastRecordedRankToday: currentSeasonRaw._lastRecordedRankToday ?? null
        };

        return {
            userId: payload.userId,
            gamesPlayed: payload.gamesPlayed ?? 0,
            gamesCompleted: payload.gamesCompleted ?? 0,
            currentStreak: payload.currentStreak ?? 0,
            maxStreak: payload.maxStreak ?? 0,
            lastPlayedDate: payload.lastPlayedDate ?? null,
            longestWordFound: payload.longestWordFound ?? null,
            totalScoreEarned: payload.totalScoreEarned ?? 0,
            averageScorePerGame: payload.averageScorePerGame ?? 0,
            completionRate: payload.completionRate ?? 0,
            currentSeason: normalizedSeason,
            seasonHistory: payload.seasonHistory ? { ...payload.seasonHistory } : {},
            dailyRankDistribution: payload.dailyRankDistribution ? { ...payload.dailyRankDistribution } : {}
        };
    }

    private checkStreakContinuity(stats: PlayerStats, today: string): void {
        if (!stats.lastPlayedDate || !isValidIsoDate(stats.lastPlayedDate)) return;

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
        const now = new Date();
        const currentYear = now.getFullYear();
        const todayStr = getTodayIsoString(now);

        const newStats: PlayerStats = {
            gamesPlayed: 0,
            gamesCompleted: 0,
            currentStreak: 0,
            maxStreak: 0,
            lastPlayedDate: null,
            currentSeason: this.createEmptySeason(currentYear),
            seasonHistory: {},
            dailyRankDistribution: {}
        };

        const gameKeys = this.storage.getKeysByPrefix(this.GAME_KEY_PREFIX);
        const gameEntries: { date: string; score: number; isCompleted: boolean; rankLabel: string | null; year: number }[] = [];

        for (const key of gameKeys) {
            const rawGameData = this.storage.load<unknown>(key);
            if (!rawGameData || typeof rawGameData !== 'object') continue;

            const rawGame = rawGameData as Record<string, unknown>;

            // Pre-normalization of legacy rank/rankLabel structures
            const extractedRankLabel =
                typeof rawGame['rankLabel'] === 'string'
                    ? rawGame['rankLabel']
                    : typeof rawGame['rank'] === 'string'
                        ? rawGame['rank']
                        : typeof rawGame['rank'] === 'object' && rawGame['rank'] !== null
                            ? ((rawGame['rank'] as { label?: string }).label ?? null)
                            : null;

            // Pre-normalization of optional invalidWords for legacy GameStates
            const rawInvalidWords = rawGame['invalidWords'];
            const normalizedInvalidWords = (rawInvalidWords === undefined || rawInvalidWords === null)
                ? []
                : rawInvalidWords;

            const normalizedGameData = {
                ...rawGame,
                rankLabel: extractedRankLabel,
                invalidWords: normalizedInvalidWords
            };

            // Perform validation on the normalized object
            if (!isValidGameState(normalizedGameData)) continue;

            const date = key.replace(this.GAME_KEY_PREFIX, '');
            if (!isValidIsoDate(date)) continue;

            const year = parseInt(date.split('-')[0], 10);
            if (isNaN(year)) continue;

            const score = typeof normalizedGameData.score === 'number' ? normalizedGameData.score : 0;
            const isCompleted = !!normalizedGameData.isCompleted;

            gameEntries.push({ date, score, isCompleted, rankLabel: extractedRankLabel, year });
        }

        // Sort the games by date chronologically
        gameEntries.sort((a, b) => {
            const [y1, m1, d1] = a.date.split('-').map(Number);
            const [y2, m2, d2] = b.date.split('-').map(Number);
            return Date.UTC(y1, m1 - 1, d1) - Date.UTC(y2, m2 - 1, d2);
        });

        let runningStreak = 0;
        let lastProcessedDate: string | null = null;
        const seasonsMap = new Map<number, SeasonStats>();

        const getSeasonForYear = (y: number): SeasonStats => {
            if (!seasonsMap.has(y)) {
                seasonsMap.set(y, this.createEmptySeason(y));
            }
            return seasonsMap.get(y)!;
        };

        for (const entry of gameEntries) {
            newStats.gamesPlayed++;
            if (entry.isCompleted) {
                newStats.gamesCompleted++;
            }

            if (entry.rankLabel) {
                if (!newStats.dailyRankDistribution) {
                    newStats.dailyRankDistribution = {};
                }
                newStats.dailyRankDistribution[entry.rankLabel] = (newStats.dailyRankDistribution[entry.rankLabel] || 0) + 1;
            }

            // Streak Management
            if (!lastProcessedDate) {
                runningStreak = 1;
            } else if (this.isConsecutiveDay(lastProcessedDate, entry.date)) {
                runningStreak++;
            } else if (lastProcessedDate !== entry.date) {
                runningStreak = 1;
            }

            newStats.maxStreak = Math.max(newStats.maxStreak, runningStreak);
            lastProcessedDate = entry.date;

            const season = getSeasonForYear(entry.year);

            // Check and apply streak milestones for the current season
            this.checkStreakMilestones(runningStreak, season);

            season.basePointsEarned += entry.score;
            season.totalSeasonPoints = season.basePointsEarned + season.bonusStreakPoints;

            // If the date matches today, initialize the daily state
            if (entry.date === todayStr && entry.year === currentYear) {
                season._lastRecordedDailyScore = entry.score;
                season._isCompletedToday = entry.isCompleted;
                season._lastRecordedRankToday = entry.rankLabel;
            }
        }

        if (gameEntries.length > 0) {
            newStats.lastPlayedDate = gameEntries[gameEntries.length - 1].date;
            newStats.currentStreak = runningStreak;
        }

        // Assign the current season and the history
        if (seasonsMap.has(currentYear)) {
            newStats.currentSeason = seasonsMap.get(currentYear)!;
            seasonsMap.delete(currentYear);
        } else {
            newStats.currentSeason = this.createEmptySeason(currentYear);
        }

        if (!newStats.seasonHistory) {
            newStats.seasonHistory = {};
        }
        seasonsMap.forEach((season, yr) => {
            newStats.seasonHistory![yr] = season;
        });

        if (newStats.currentSeason) {
            newStats.currentSeason.highestTierAchieved = this.calculateTier(newStats);
        }

        return newStats;
    }

    private saveStats(statsToSave?: PlayerStats): void {
        this.storage.save<PlayerStats>(this.STORAGE_KEY, statsToSave ?? this._stats());
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