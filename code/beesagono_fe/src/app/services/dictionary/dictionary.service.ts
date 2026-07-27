import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class DictionaryService {
  private dictionarySet: Set<string> | null = null;
  private dictionaryList: string[] | null = null;

  /**
   * Fetches, validates and parses dictionary.json once; cached for subsequent calls.
   */
  async loadDictionary(): Promise<string[]> {
    if (this.dictionaryList) {
      return this.dictionaryList;
    }

    try {
      const response = await fetch('assets/dictionary.json');
      if (!response.ok) {
        throw new Error(`Impossibile caricare il dizionario (HTTP ${response.status})`);
      }

      const rawData: unknown = await response.json();

      if (!Array.isArray(rawData)) {
        throw new Error('Il formato del dizionario non è un array di stringhe valido.');
      }

      // Sanitize and filter: keep only valid uppercase alphabetic strings
      const sanitized: string[] = [];
      for (let i = 0; i < rawData.length; i++) {
        const item = rawData[i];
        if (typeof item === 'string') {
          const clean = item.trim().toUpperCase();
          // Solo lettere A-Z italiane/inglesi
          if (/^[A-Z]+$/.test(clean)) {
            sanitized.push(clean);
          }
        }
      }

      if (sanitized.length === 0) {
        throw new Error('Il file dictionary.json non contiene parole valide.');
      }

      this.dictionaryList = Array.from(new Set(sanitized)); // Rimuove eventuali duplicati
      this.dictionarySet = new Set(this.dictionaryList);

      return this.dictionaryList;
    } catch (error) {
      console.error('[DictionaryService] Errore critico nel caricamento:', error);
      throw error;
    }
  }

  /**
   * Returns the O(1)-lookup Set built from the loaded word list.
   */
  getWordSet(): Set<string> {
    if (!this.dictionarySet) {
      throw new Error('DictionaryService.getWordSet() chiamato prima del completamento di loadDictionary().');
    }
    return this.dictionarySet;
  }
}