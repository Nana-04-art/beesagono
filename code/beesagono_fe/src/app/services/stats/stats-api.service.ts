import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  LeaderboardEntryDto,
  PlayerSeasonResponse,
  PlayerStatsResponse,
  RankDistributionResponse
} from '../../models/stats/stats-dto.model';
import { environment } from '../../environments/environment';
import { StatsService } from './stats.service';
import { PlayerStats } from '../../models/stats/stats.model';

@Injectable({
  providedIn: 'root'
})
export class StatsApiService {
  private readonly http = inject(HttpClient);
  private readonly statsService = inject(StatsService);
  private readonly statsUrl = `${environment.apiBaseUrl}/stats`;
  private readonly seasonsUrl = `${environment.apiBaseUrl}/seasons`;

  readonly myStats = signal<PlayerStatsResponse | null>(null);
  readonly currentSeason = signal<PlayerSeasonResponse | null>(null);
  readonly leaderboard = signal<LeaderboardEntryDto[]>([]);
  readonly rankDistribution = signal<RankDistributionResponse | null>(null);
  readonly isLoading = signal<boolean>(false);

  // -- GET MY STATS METHOD --
  getMyStats(): Observable<PlayerStatsResponse> {
    this.isLoading.set(true);
    return this.http.get<PlayerStatsResponse>(`${this.statsUrl}/me`).pipe(
      tap({
        next: (stats) => {
          this.myStats.set(stats);
          this.statsService.syncWithRemoteStats(stats as unknown as PlayerStats);
          this.isLoading.set(false);
        },
        error: () => this.isLoading.set(false)
      })
    );
  }

  // -- SYNC STATS WITH SERVER METHOD --
  syncStatsWithServer(localStats: PlayerStats): Observable<PlayerStatsResponse> {
    this.isLoading.set(true);

    // Map Angular model keys to Java request DTO
    const payload = {
      gamesPlayed: localStats.gamesPlayed,
      gamesCompleted: localStats.gamesCompleted,
      currentStreak: localStats.currentStreak,
      maxStreak: localStats.maxStreak,
      lastPlayedDate: localStats.lastPlayedDate,
      dailyRankDistribution: localStats.dailyRankDistribution
    };

    return this.http.post<PlayerStatsResponse>(`${this.statsUrl}/sync`, payload).pipe(
      tap({
        next: (stats) => {
          this.myStats.set(stats);
          this.statsService.syncWithRemoteStats(stats as unknown as PlayerStats);
          this.isLoading.set(false);
        },
        error: () => this.isLoading.set(false)
      })
    );
  }

  // -- GET CURRENT SEASON METHOD --  
  getCurrentSeason(): Observable<PlayerSeasonResponse> {
    return this.http.get<PlayerSeasonResponse>(`${this.seasonsUrl}/me`).pipe(
      tap((season) => this.currentSeason.set(season))
    );
  }

  // -- GET LEADERBOARD METHOD --
  getLeaderboard(limit = 10): Observable<LeaderboardEntryDto[]> {
    return this.http.get<LeaderboardEntryDto[]>(`${this.seasonsUrl}/leaderboard`, {
      params: { limit }
    }).pipe(
      tap((data) => this.leaderboard.set(data))
    );
  }

  // -- GET RANK DISTRIBUTION METHOD --
  getRankDistribution(): Observable<RankDistributionResponse> {
    return this.http.get<RankDistributionResponse>(`${this.seasonsUrl}/rank-distribution`).pipe(
      tap((data) => this.rankDistribution.set(data))
    );
  }
}