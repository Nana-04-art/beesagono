import { TestBed } from '@angular/core/testing';
import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { StatsService } from './stats.service';
import { StorageService } from '../storage/storage.service';
import { PlayerStats } from '../../models/stats.model';
import { CAREER_TIERS, STREAK_MILESTONES } from '../../config/career-tiers.constant';

describe('StatsService', () => {
    let service: StatsService;
    let mockStorageService: {
        load: ReturnType<typeof vi.fn>;
        save: ReturnType<typeof vi.fn>;
        getKeysByPrefix: ReturnType<typeof vi.fn>;
    };

    beforeEach(() => {
        mockStorageService = {
            load: vi.fn().mockReturnValue(null),
            save: vi.fn().mockReturnValue(true),
            getKeysByPrefix: vi.fn().mockReturnValue([]),
        };

        TestBed.configureTestingModule({
            providers: [
                StatsService,
                { provide: StorageService, useValue: mockStorageService },
            ],
        });

        service = TestBed.inject(StatsService);
    });

    afterEach(() => {
        vi.restoreAllMocks();
        localStorage.clear();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should initialize with default empty stats when storage is empty', () => {
        const stats = service.stats();
        expect(stats.gamesPlayed).toBe(0);
        expect(stats.currentStreak).toBe(0);
        expect(stats.maxStreak).toBe(0);
        expect(stats.lastPlayedDate).toBeNull();
        expect(stats.currentSeason.year).toBe(new Date().getFullYear());
    });

    it('should record game started and update stats correctly for the first play', () => {
        service.recordGameStarted('2026-08-13');

        const stats = service.stats();
        expect(stats.gamesPlayed).toBe(1);
        expect(stats.currentStreak).toBe(1);
        expect(stats.maxStreak).toBe(1);
        expect(stats.lastPlayedDate).toBe('2026-08-13');
        expect(mockStorageService.save).toHaveBeenCalled();
    });

    it('should increment streak correctly on consecutive days', () => {
        service.recordGameStarted('2026-08-12');
        expect(service.stats().currentStreak).toBe(1);

        service.recordProgress('2026-08-13', 10, false, 'Buono');

        const stats = service.stats();
        expect(stats.gamesPlayed).toBe(2);
        expect(stats.currentStreak).toBe(2);
        expect(stats.maxStreak).toBe(2);
        expect(stats.lastPlayedDate).toBe('2026-08-13');
        expect(stats.dailyRankDistribution['Buono']).toBe(1);
    });

    it('should reset streak to 1 if days are missed', () => {
        service.recordGameStarted('2026-08-10');
        expect(service.stats().currentStreak).toBe(1);

        service.recordProgress('2026-08-13', 15, true, 'Ottimo');

        const stats = service.stats();
        expect(stats.gamesPlayed).toBe(2);
        expect(stats.currentStreak).toBe(1);
        expect(stats.maxStreak).toBe(1);
        expect(stats.gamesCompleted).toBe(1);
    });

    it('should not increment gamesPlayed or streak if playing multiple times on the same day', () => {
        service.recordGameStarted('2026-08-13');
        service.recordProgress('2026-08-13', 20, true, 'Genio');

        const stats = service.stats();
        expect(stats.gamesPlayed).toBe(1);
        expect(stats.currentStreak).toBe(1);
        expect(stats.gamesCompleted).toBe(1);
        expect(stats.currentSeason.basePointsEarned).toBe(20);
    });

    it('should handle rank promotion on the same day correctly', () => {
        service.recordProgress('2026-08-13', 10, false, 'Principiante');
        expect(service.stats().dailyRankDistribution['Principiante']).toBe(1);

        service.recordProgress('2026-08-13', 25, false, 'Eccellente');

        const stats = service.stats();
        expect(stats.dailyRankDistribution['Principiante']).toBe(0);
        expect(stats.dailyRankDistribution['Eccellente']).toBe(1);
    });

    it('should award streak milestone bonus points when milestone is hit', () => {
        const days = ['2026-08-01', '2026-08-02', '2026-08-03'];
        days.forEach(day => service.recordGameStarted(day));

        const stats = service.stats();
        expect(stats.currentStreak).toBe(3);
        expect(stats.currentSeason.claimedStreakMilestones).toContain(3);
        expect(stats.currentSeason.bonusStreakPoints).toBe(STREAK_MILESTONES[3]);
        expect(stats.currentSeason.totalSeasonPoints).toBeGreaterThanOrEqual(STREAK_MILESTONES[3]);
    });

    it('should handle season/year rollover correctly', () => {
        service.recordProgress('2025-12-31', 50, true, 'Genio');
        expect(service.stats().currentSeason.year).toBe(2025);

        service.recordProgress('2026-01-01', 10, false, 'Principiante');

        const stats = service.stats();
        expect(stats.currentSeason.year).toBe(2026);
        expect(stats.seasonHistory[2025]).toBeDefined();
        expect(stats.seasonHistory[2025].basePointsEarned).toBe(50);
        expect(stats.currentSeason.basePointsEarned).toBe(10);
    });

    it('should reset currentStreak on initialization if lastPlayedDate is older than 1 day', () => {
        const staleStats: PlayerStats = {
            gamesPlayed: 5,
            gamesCompleted: 2,
            currentStreak: 5,
            maxStreak: 5,
            lastPlayedDate: '2026-08-01',
            currentSeason: {
                year: 2026,
                basePointsEarned: 100,
                bonusStreakPoints: 0,
                totalSeasonPoints: 100,
                highestTierAchieved: CAREER_TIERS[0].name,
                claimedStreakMilestones: []
            },
            seasonHistory: {},
            dailyRankDistribution: {}
        };

        mockStorageService.load.mockReturnValue(staleStats);

        TestBed.resetTestingModule();
        TestBed.configureTestingModule({
            providers: [
                StatsService,
                { provide: StorageService, useValue: mockStorageService },
            ],
        });

        const newService = TestBed.inject(StatsService);

        expect(mockStorageService.load).toHaveBeenCalledWith('stats');
        expect(newService.stats().currentStreak).toBe(0);
    });

    it('should rebuild stats from StorageService game keys when main stats key is missing', () => {
        mockStorageService.getKeysByPrefix.mockReturnValue([
            'beesagono:game:2026-08-10',
            'beesagono:game:2026-08-11',
        ]);

        mockStorageService.load.mockImplementation((key: string) => {
            if (key === 'stats') return null;
            if (key === 'beesagono:game:2026-08-10') {
                return {
                    version: 1,
                    date: '2026-08-10',
                    score: 10,
                    isCompleted: false,
                    foundWords: ['CASA'],
                    invalidWords: [],
                    rankLabel: 'Buono'
                };
            }
            if (key === 'beesagono:game:2026-08-11') {
                return {
                    version: 1,
                    date: '2026-08-11',
                    score: 20,
                    isCompleted: true,
                    foundWords: ['MARE', 'SOLE'],
                    invalidWords: [],
                    rankLabel: 'Genio'
                };
            }
            return null;
        });

        TestBed.resetTestingModule();
        TestBed.configureTestingModule({
            providers: [
                StatsService,
                { provide: StorageService, useValue: mockStorageService },
            ],
        });

        const newService = TestBed.inject(StatsService);
        const stats = newService.stats();

        expect(mockStorageService.getKeysByPrefix).toHaveBeenCalledWith('beesagono:game:');
        expect(stats.gamesPlayed).toBe(2);
        expect(stats.gamesCompleted).toBe(1);
        expect(stats.maxStreak).toBe(2);
        expect(stats.lastPlayedDate).toBe('2026-08-11');
        expect(stats.dailyRankDistribution['Buono']).toBe(1);
        expect(stats.dailyRankDistribution['Genio']).toBe(1);
    });
});