import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { of } from 'rxjs';

import { SyncService } from './sync.service';
import { StorageService } from '../storage/storage.service';
import { StatsApiService } from '../stats/stats-api.service';
import { StatsService } from '../stats/stats.service';
import { environment } from '../../environments/environment';
import { GameState } from '../../models/game/game-state.model';
import { PlayerStats } from '../../models/stats/stats.model';

describe('SyncService', () => {
  let service: SyncService;
  let httpTestingController: HttpTestingController;

  let mockStorageService: {
    getKeysByPrefix: ReturnType<typeof vi.fn>;
    load: ReturnType<typeof vi.fn>;
    clear: ReturnType<typeof vi.fn>;
  };

  let mockStatsApiService: {
    syncStatsWithServer: ReturnType<typeof vi.fn>;
    getMyStats: ReturnType<typeof vi.fn>;
  };

  let mockStatsService: {
    syncWithRemoteStats: ReturnType<typeof vi.fn>;
  };

  const mockServerStats = {
    userId: 'user-123',
    gamesPlayed: 5,
    gamesCompleted: 3,
    currentStreak: 2,
    maxStreak: 4,
    totalScoreEarned: 150,
  } as unknown as PlayerStats;

  beforeEach(() => {
    mockStorageService = {
      getKeysByPrefix: vi.fn().mockReturnValue([]),
      load: vi.fn().mockReturnValue(null),
      clear: vi.fn().mockReturnValue(true),
    };

    mockStatsApiService = {
      syncStatsWithServer: vi.fn(),
      getMyStats: vi.fn(),
    };

    mockStatsService = {
      syncWithRemoteStats: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        SyncService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: StorageService, useValue: mockStorageService },
        { provide: StatsApiService, useValue: mockStatsApiService },
        { provide: StatsService, useValue: mockStatsService },
      ],
    });

    service = TestBed.inject(SyncService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    if (httpTestingController) {
      httpTestingController.verify();
    }
    vi.restoreAllMocks();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('syncLocalProgressWithServer', () => {
    it('should fetch stats from server if no local games or local stats exist', () => {
      mockStorageService.getKeysByPrefix.mockReturnValue([]);
      mockStorageService.load.mockImplementation((key: string) => (key === 'stats' ? null : null));
      mockStatsApiService.getMyStats.mockReturnValue(of(mockServerStats));

      let response: PlayerStats | undefined;
      service.syncLocalProgressWithServer().subscribe((res) => {
        response = res;
      });

      expect(response).toEqual(mockServerStats);
      expect(mockStatsApiService.getMyStats).toHaveBeenCalledTimes(1);
      expect(mockStatsService.syncWithRemoteStats).toHaveBeenCalledWith(mockServerStats);
    });

    it('should sync local stats with server if local stats exist but no local games exist', () => {
      const mockLocalStats = { userId: 'user-123' } as PlayerStats;
      mockStorageService.getKeysByPrefix.mockReturnValue([]);
      mockStorageService.load.mockImplementation((key: string) => (key === 'stats' ? mockLocalStats : null));
      mockStatsApiService.syncStatsWithServer.mockReturnValue(of(mockServerStats));

      let response: PlayerStats | undefined;
      service.syncLocalProgressWithServer().subscribe((res) => {
        response = res;
      });

      expect(response).toEqual(mockServerStats);
      expect(mockStatsApiService.syncStatsWithServer).toHaveBeenCalledWith(mockLocalStats);
      expect(mockStatsService.syncWithRemoteStats).toHaveBeenCalledWith(mockServerStats);
    });

    it('should send bulk game payload, clear local storage on success, and sync stats', () => {
      const rawKeys = ['beesagono:game:2026-10-07'];
      const mockGameState: Partial<GameState> = {
        date: '2026-10-07',
        centerLetter: 'A',
        outerLetters: ['B', 'C', 'D', 'E', 'F', 'G'],
        foundWords: ['CASA'],
        score: 10,
        isCompleted: false,
      };

      mockStorageService.getKeysByPrefix.mockReturnValue(rawKeys);
      mockStorageService.load.mockImplementation((key: string) => {
        if (key === 'game:2026-10-07') {
          return mockGameState as GameState;
        }
        return null;
      });
      mockStatsApiService.getMyStats.mockReturnValue(of(mockServerStats));

      let response: PlayerStats | undefined;
      service.syncLocalProgressWithServer().subscribe((res) => {
        response = res;
      });

      const req = httpTestingController.expectOne(`${environment.apiBaseUrl}/game/sync`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.games.length).toBe(1);
      expect(req.request.body.games[0].puzzleDate).toBe('2026-10-07');

      req.flush([{ id: 'session-1' }]);

      expect(response).toEqual(mockServerStats);
      expect(mockStorageService.clear).toHaveBeenCalledWith('game:2026-10-07');
      expect(mockStorageService.clear).toHaveBeenCalledWith('state');
      expect(mockStatsService.syncWithRemoteStats).toHaveBeenCalledWith(mockServerStats);
    });
  });
});