import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { DailyPuzzleResponse } from '../../models/puzzle/daily-puzzle-response.model';
import { GameBoard } from '../../models/game/game-board.model';
import { Cell } from '../../models/game/cell.model';
import { HexPosition } from '../../models/game/hex-position.type';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class PuzzleService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/puzzles`;

  /**
   * Fetches today's puzzle from the backend and converts it into a GameBoard model.
   */
  getTodayPuzzle(): Observable<GameBoard> {
    return this.http.get<DailyPuzzleResponse>(`${this.baseUrl}/today`).pipe(
      map((response) => this.mapToGameBoard(response))
    );
  }

  // -- Helper Method -- 
  private mapToGameBoard(dto: DailyPuzzleResponse): GameBoard {
    const centerLetter = dto.centerLetter.toUpperCase();
    const outerLettersArray = Array.from(dto.outerLetters || []).map((l) => l.toUpperCase());

    const cells: Cell[] = [
      {
        id: 'hex-0',
        letter: centerLetter,
        position: 0 as HexPosition,
        isCenter: true,
      },
      ...outerLettersArray.map((letter, index) => ({
        id: `hex-${index + 1}`,
        letter,
        position: (index + 1) as HexPosition,
        isCenter: false,
      })),
    ];

    return {
      date: dto.puzzleDate,
      seed: dto.id || dto.puzzleDate,
      cells,
      possibleWords: dto.validWords || [],
      mielegrammi: dto.mielegrammi || [],
      maxScore: dto.maxScore,
    };
  }
}