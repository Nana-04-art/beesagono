import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

interface DictionaryFile {
  words: string[];
}

@Injectable({ providedIn: 'root' })
export class DictionaryService {
  private readonly http = inject(HttpClient);

  private wordsPromise: Promise<string[]> | null = null;

  private wordSet: Set<string> | null = null;

  /** Fetches and parses dictionary.json once; cached for subsequent calls. */
  loadDictionary(): Promise<string[]> {
    if (this.wordsPromise) {
      return this.wordsPromise;
    }

    this.wordsPromise = firstValueFrom(
      this.http.get<DictionaryFile>('/dictionary.json')
    )
      .then((data) => {
        const words = this.validateAndNormalize(data);
        this.wordSet = new Set(words);
        return words;
      })
      .catch((error: unknown) => {
        this.wordsPromise = null;
        const message = error instanceof Error ? error.message : String(error);
        throw new Error(`DictionaryService: failed to load dictionary.json — ${message}`);
      });

    return this.wordsPromise;
  }

  /** Returns the O(1)-lookup Set built from the loaded word list. Throws if called before loadDictionary() resolves. */
  getWordSet(): Set<string> {
    if (!this.wordSet) {
      throw new Error(
        'DictionaryService: getWordSet() called before loadDictionary() resolved. ' +
        'Await loadDictionary() first.'
      );
    }
    return this.wordSet;
  }

  private validateAndNormalize(data: DictionaryFile): string[] {
    if (!data || !Array.isArray(data.words)) {
      throw new Error(
        'DictionaryService: dictionary.json has an unexpected shape (expected { words: string[] }).'
      );
    }

    return data.words
      .map((w) => (typeof w === 'string' ? w.trim().toUpperCase() : ''))
      .filter((w) => w.length > 0);
  }
}