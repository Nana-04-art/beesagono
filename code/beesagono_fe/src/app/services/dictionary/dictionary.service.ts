import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class DictionaryService {
  private dictionarySet: Set<string> | null = null;
  private dictionaryList: string[] | null = null;

  // Fetches, validates and parses dictionary.json once; cached for subsequent calls.
  async loadDictionary(): Promise<string[]> {
    if (this.dictionaryList) {
      return this.dictionaryList;
    }

    try {
      const response = await fetch('dictionary.json');
      if (!response.ok) {
        throw new Error(`Impossibile caricare il dizionario (HTTP ${response.status})`);
      }

      const rawData: unknown = await response.json();

      // The array is extracted whether the JSON is a native array or an object { "words": [...] }
      let wordsArray: unknown[] = [];

      if (Array.isArray(rawData)) {
        wordsArray = rawData;
      } else if (rawData && typeof rawData === 'object' && 'words' in rawData && Array.isArray((rawData as { words: unknown }).words)) {
        wordsArray = (rawData as { words: unknown[] }).words;
      } else {
        throw new Error('Il formato del dizionario non è valido (deve essere un array o un oggetto con chiave "words").');
      }

      // Sanitize and filter: keep only valid uppercase alphabetic strings
      const sanitized: string[] = [];
      for (let i = 0; i < wordsArray.length; i++) {
        const item = wordsArray[i];
        if (typeof item === 'string') {
          const clean = item.trim().toUpperCase();
          if (/^[A-Z]+$/.test(clean)) {
            sanitized.push(clean);
          }
        }
      }

      if (sanitized.length === 0) {
        throw new Error('Il file dictionary.json non contiene parole valide.');
      }

      this.dictionaryList = Array.from(new Set(sanitized));
      this.dictionarySet = new Set(this.dictionaryList);

      return this.dictionaryList;
    } catch (error) {
      console.error('[DictionaryService] Errore critico nel caricamento:', error);
      throw error;
    }
  }

  // Returns the O(1)-lookup Set built from the loaded word list.
  getWordSet(): Set<string> {
    if (!this.dictionarySet) {
      throw new Error('DictionaryService.getWordSet() chiamato prima del completamento di loadDictionary().');
    }
    return this.dictionarySet;
  }
}