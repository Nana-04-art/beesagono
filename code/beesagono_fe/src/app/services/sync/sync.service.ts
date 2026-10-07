import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, switchMap, tap } from 'rxjs';
import { StorageService } from '../storage/storage.service';
import { StatsApiService } from '../stats/stats-api.service';
import { StatsService } from '../stats/stats.service';
import { GameBulkSyncRequest, GameSyncRequest } from '../../models/sync/game-sync.model';
import { GameSessionResponse } from '../../models/game/game-dto.model';
import { environment } from '../../environments/environment';
import { GameState } from '../../models/game/game-state.model';
import { PlayerStats } from '../../models/stats/stats.model';

@Injectable({
  providedIn: 'root',
})
export class SyncService {
  private readonly http = inject(HttpClient);
  private readonly storageService = inject(StorageService);
  private readonly statsApiService = inject(StatsApiService);
  private readonly statsService = inject(StatsService);
  private readonly baseUrl = `${environment.apiBaseUrl}/game`;

  syncLocalProgressWithServer(): Observable<any> {
    const rawGameKeys = this.storageService.getKeysByPrefix('game:');
    const localStats = this.storageService.load<PlayerStats>('stats');

    const syncRequests: GameSyncRequest[] = [];

    if (rawGameKeys && rawGameKeys.length > 0) {
      for (const rawKey of rawGameKeys) {
        const keyForStorage = rawKey.replace(/^beesagono:/, '');
        const savedState = this.storageService.load<GameState>(keyForStorage);

        if (savedState && (savedState.date || (savedState as any).puzzleDate)) {
          const puzzleDate = savedState.date || (savedState as any).puzzleDate;

          syncRequests.push({
            puzzleDate: puzzleDate,
            centerLetter: savedState.centerLetter,
            outerLetters: savedState.outerLetters,
            foundWords: savedState.foundWords || [],
            invalidWords: savedState.invalidWords || [],
            foundMielegrammi: savedState.foundMielegrammi || [],
            currentScore: savedState.score || 0,
            currentRankLabel: savedState.rankLabel,
            startTime: savedState.startTime,
            lastUpdated: savedState.lastUpdated,
            completionPercentage: savedState.completionPercentage ?? (savedState.isCompleted ? 1.0 : undefined),
            isCompleted: savedState.isCompleted ?? false
          });
        }
      }
    }

    // Helper to handle stats synchronization response and update client state
    const handleStatsSync = () => {
      const statsObservable = localStats
        ? this.statsApiService.syncStatsWithServer(localStats)
        : this.statsApiService.getMyStats();

      return statsObservable.pipe(
        tap((serverStats: any) => {
          // Overwrite localStorage with authoritative data calculated by backend
          if (serverStats) {
            this.statsService.syncWithRemoteStats(serverStats as unknown as PlayerStats);
          }
        })
      );
    };

    // If there are games to synchronize
    if (syncRequests.length > 0) {
      const bulkPayload: GameBulkSyncRequest = { games: syncRequests };

      return this.http.post<GameSessionResponse[]>(`${this.baseUrl}/sync`, bulkPayload).pipe(
        tap(() => {
          for (const rawKey of rawGameKeys) {
            const keyForStorage = rawKey.startsWith('beesagono:') ? rawKey.replace('beesagono:', '') : rawKey;
            this.storageService.clear(keyForStorage);
          }
          this.storageService.clear('state');
        }),
        switchMap(() => handleStatsSync())
      );
    }

    // If there are no games to synchronize but we need to sync stats or fetch them
    return handleStatsSync();
  }
}