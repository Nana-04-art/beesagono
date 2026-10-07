import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { WordValidationRequest, WordValidationResponse } from '../../models/game/game-dto.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DictionaryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/dictionary`;

  /**
   * Sends the word to the backend for validation.
   * The backend will check length, central letter, the puzzle date and presence in the DB.
   */
  validateWord(word: string): Observable<WordValidationResponse> {
    const payload: WordValidationRequest = {
      word: word.trim().toUpperCase()
    };

    return this.http.post<WordValidationResponse>(`${this.baseUrl}/validate`, payload);
  }
}