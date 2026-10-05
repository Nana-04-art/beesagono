import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { WordValidationRequest, WordValidationResponse } from '../../models/game/word-validation.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DictionaryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/dictionary`;

  /**
   * Sends the word to the backend for validation based on the puzzle date.
   * The backend will check length, central letter, and presence in the DB.
   */
  validateWord(puzzleDate: string, word: string): Observable<WordValidationResponse> {
    const payload: WordValidationRequest = {
      puzzleDate,
      word: word.trim().toUpperCase()
    };

    return this.http.post<WordValidationResponse>(`${this.baseUrl}/validate`, payload);
  }
}