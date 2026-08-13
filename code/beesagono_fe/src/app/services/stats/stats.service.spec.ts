import { TestBed } from '@angular/core/testing';
import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { StatsService } from './stats.service';
import { StorageService } from '../storage/storage.service';
import { PlayerStats } from '../../models/stats.model';

describe('StatsService', () => {
    let service: StatsService;
    let mockStorageService: any;

    beforeEach(() => {
        mockStorageService = {
            load: vi.fn().mockReturnValue(null),
            save: vi.fn().mockReturnValue(true),
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
    });

    it('should record game started and update stats correctly for the first play', () => {
        service.recordGameStarted('2026-08-13');

        const stats = service.stats();
        expect(stats.gamesPlayed).toBe(1);
        expect(stats.currentStreak).toBe(1);
        expect(stats.maxStreak).toBe(1);
        expect(stats.lastPlayedDate).toBe('2026-08-13');
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
});