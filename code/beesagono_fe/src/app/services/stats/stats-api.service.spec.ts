import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';

import { StatsApiService } from './stats-api.service';
import { StatsService } from './stats.service';
import { environment } from '../../environments/environment';
import {
  PlayerSeasonResponse,
  PlayerStatsResponse,
  LeaderboardEntryDto,
  RankDistributionResponse
} from '../../models/stats/stats-dto.model';
import { PlayerStats } from '../../models/stats/stats.model';

describe('StatsApiService', () => {
  let service: StatsApiService;
  let httpMock: HttpTestingController;
  let mockStatsService: { syncWithRemoteStats: ReturnType<typeof vi.fn> };

  const statsUrl = `${environment.apiBaseUrl}/stats`;
  const seasonsUrl = `${environment.apiBaseUrl}/seasons`;

  const mockPlayerStatsResponse: PlayerStatsResponse = {
    userId: 'user-123',
    gamesPlayed: 10,
    gamesCompleted: 8,
    currentStreak: 3,
    maxStreak: 5,
    longestWordFound: 'BEESAGONO',
    totalScoreEarned: 500,
    averageScorePerGame: 50,
    completionRate: 0.8
  };

  const mockPlayerStats: PlayerStats = {
    userId: 'user-123',
    gamesPlayed: 10,
    gamesCompleted: 8,
    currentStreak: 3,
    maxStreak: 5,
    lastPlayedDate: '2026-03-30',
    dailyRankDistribution: { APPRENDISTA: 2, GENIO: 6 },
    totalScoreEarned: 500
  };

  beforeEach(() => {
    mockStatsService = {
      syncWithRemoteStats: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        StatsApiService,
        { provide: StatsService, useValue: mockStatsService }
      ]
    });

    service = TestBed.inject(StatsApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getMyStats', () => {
    it('should fetch user stats, update signals and call statsService.syncWithRemoteStats', () => {
      service.getMyStats().subscribe((stats) => {
        expect(stats).toEqual(mockPlayerStatsResponse);
      });

      expect(service.isLoading()).toBe(true);

      const req = httpMock.expectOne(`${statsUrl}/me`);
      expect(req.request.method).toBe('GET');

      req.flush(mockPlayerStatsResponse);

      expect(service.myStats()).toEqual(mockPlayerStatsResponse);
      expect(mockStatsService.syncWithRemoteStats).toHaveBeenCalledWith(mockPlayerStatsResponse);
      expect(service.isLoading()).toBe(false);
    });

    it('should handle error and reset isLoading signal', () => {
      service.getMyStats().subscribe({
        next: () => expect.fail('Should fail with HTTP error'),
        error: (error) => expect(error.status).toBe(500)
      });

      expect(service.isLoading()).toBe(true);

      const req = httpMock.expectOne(`${statsUrl}/me`);
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

      expect(service.isLoading()).toBe(false);
    });
  });

  describe('syncStatsWithServer', () => {
    it('should send local stats payload, update myStats signal and sync statsService', () => {
      service.syncStatsWithServer(mockPlayerStats).subscribe((response) => {
        expect(response).toEqual(mockPlayerStatsResponse);
      });

      expect(service.isLoading()).toBe(true);

      const req = httpMock.expectOne(`${statsUrl}/sync`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        gamesPlayed: mockPlayerStats.gamesPlayed,
        gamesCompleted: mockPlayerStats.gamesCompleted,
        currentStreak: mockPlayerStats.currentStreak,
        maxStreak: mockPlayerStats.maxStreak,
        lastPlayedDate: mockPlayerStats.lastPlayedDate,
        dailyRankDistribution: mockPlayerStats.dailyRankDistribution
      });

      req.flush(mockPlayerStatsResponse);

      expect(service.myStats()).toEqual(mockPlayerStatsResponse);
      expect(mockStatsService.syncWithRemoteStats).toHaveBeenCalledWith(mockPlayerStatsResponse);
      expect(service.isLoading()).toBe(false);
    });

    it('should handle sync error and set isLoading to false', () => {
      service.syncStatsWithServer(mockPlayerStats).subscribe({
        next: () => expect.fail('Should fail with HTTP error'),
        error: (error) => expect(error.status).toBe(400)
      });

      expect(service.isLoading()).toBe(true);

      const req = httpMock.expectOne(`${statsUrl}/sync`);
      req.flush('Bad Request', { status: 400, statusText: 'Bad Request' });

      expect(service.isLoading()).toBe(false);
    });
  });

  describe('getCurrentSeason', () => {
    it('should fetch current season and update currentSeason signal', () => {
      const mockSeason: PlayerSeasonResponse = {
        year: 2026,
        highestTierAchieved: 'DIAMOND',
        gamesPlayed: 15,
        gamesCompleted: 12,
        currentStreak: 4,
        maxStreak: 7,
        basePoints: 1000,
        bonusPoints: 200,
        totalPoints: 1200
      };

      service.getCurrentSeason().subscribe((season) => {
        expect(season).toEqual(mockSeason);
      });

      const req = httpMock.expectOne(`${seasonsUrl}/me`);
      expect(req.request.method).toBe('GET');

      req.flush(mockSeason);

      expect(service.currentSeason()).toEqual(mockSeason);
    });
  });

  describe('getLeaderboard', () => {
    it('should fetch leaderboard with default limit parameter and update leaderboard signal', () => {
      const mockLeaderboard: LeaderboardEntryDto[] = [
        {
          username: 'Player1',
          totalPoints: 1000,
          highestTierAchieved: 'DIAMOND',
          rankPosition: 1
        }
      ];

      service.getLeaderboard().subscribe((data) => {
        expect(data).toEqual(mockLeaderboard);
      });

      const req = httpMock.expectOne(`${seasonsUrl}/leaderboard?limit=10`);
      expect(req.request.method).toBe('GET');

      req.flush(mockLeaderboard);

      expect(service.leaderboard()).toEqual(mockLeaderboard);
    });

    it('should fetch leaderboard with custom limit parameter', () => {
      service.getLeaderboard(25).subscribe();

      const req = httpMock.expectOne(`${seasonsUrl}/leaderboard?limit=25`);
      expect(req.request.method).toBe('GET');

      req.flush([]);
    });
  });

  describe('getRankDistribution', () => {
    it('should fetch rank distribution and update rankDistribution signal', () => {
      const mockDistribution: RankDistributionResponse = {
        BEGINNER: 10,
        GENIUS: 2
      } as unknown as RankDistributionResponse;

      service.getRankDistribution().subscribe((data) => {
        expect(data).toEqual(mockDistribution);
      });

      const req = httpMock.expectOne(`${seasonsUrl}/rank-distribution`);
      expect(req.request.method).toBe('GET');

      req.flush(mockDistribution);

      expect(service.rankDistribution()).toEqual(mockDistribution);
    });
  });
});