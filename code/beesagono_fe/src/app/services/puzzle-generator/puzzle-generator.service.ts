import { Injectable } from '@angular/core';
import { Cell } from '../../models/cell.model';
import { GameBoard } from '../../models/game-board.model';
import { HexPosition } from '../../models/hex-position.type';
import { GAME_RULES } from '../../config/game-rules.config';

@Injectable({ providedIn: 'root' })
export class PuzzleGeneratorService {
  /**
   * Generates a deterministic daily puzzle using the Candidate Pangrams Strategy.
   * Safe against undefined or empty word sets.
   */
  generateDailyPuzzle(date: string, wordSet: Set<string> | string[] = new Set()): GameBoard {
    // Safely handle undefined, null, Set, or Array inputs
    const dictionary = Array.isArray(wordSet)
      ? wordSet
      : Array.from(wordSet ?? []);

    // Extract all pangram candidates (words with exactly 7 unique letters)
    const pangramCandidates = this.extractPangrams(dictionary);

    // Fallback: If dictionary/pangramCandidates is empty (e.g. during tests or initial load),
    // provide a safe mock pangram to prevent runtime exceptions
    const safeCandidates = pangramCandidates.length > 0
      ? pangramCandidates
      : ['BEESAGO'];

    const baseSeed = this.hashDateString(date);

    const rng = this.mulberry32(baseSeed);

    let lastCandidateBoard: GameBoard | null = null;

    // Generation Loop (Max MAX_GENERATION_ATTEMPTS)
    for (let attempt = 0; attempt < GAME_RULES.MAX_GENERATION_ATTEMPTS; attempt++) {
      // Pick deterministic candidate pangram
      const targetPangram = safeCandidates[Math.floor(rng() * safeCandidates.length)];

      // Extract unique 7 letters and sort them alphabetically for absolute determinism
      const uniqueLetters = Array.from(new Set(targetPangram)).sort((a, b) => a.localeCompare(b));

      // Deterministically pick 1 center letter out of the 7
      const centerLetter = uniqueLetters[Math.floor(rng() * uniqueLetters.length)];

      // Compute valid target words and mielegrammi for this board setup
      const candidateSet = new Set(uniqueLetters);
      const targetWords: string[] = [];
      const mielegrammi: string[] = [];

      for (let i = 0; i < dictionary.length; i++) {
        const word = dictionary[i];

        if (
          word.length >= GAME_RULES.MIN_WORD_LENGTH &&
          word.includes(centerLetter) &&
          this.wordUsesOnlySet(word, candidateSet)
        ) {
          targetWords.push(word);

          const wordUniqueLetters = new Set(word);
          if (wordUniqueLetters.size === GAME_RULES.REQUIRED_LETTERS_COUNT) {
            mielegrammi.push(word);
          }
        }
      }

      // Quality Gate check
      if (
        targetWords.length >= GAME_RULES.MIN_TARGET_WORDS_COUNT &&
        mielegrammi.length >= GAME_RULES.MIN_MIELEGRAMMI_COUNT
      ) {
        return this.buildGameBoard(date, `${baseSeed}_${attempt}`, uniqueLetters, centerLetter, targetWords, mielegrammi);
      }

      // Keep the last board generated as fallback if Quality Gate is never met
      if (attempt === GAME_RULES.MAX_GENERATION_ATTEMPTS - 1) {
        lastCandidateBoard = this.buildGameBoard(
          date,
          `${baseSeed}_${attempt}`,
          uniqueLetters,
          centerLetter,
          targetWords,
          mielegrammi
        );
      }
    }

    return lastCandidateBoard!;
  }

  // --- Frozen Algorithms ---

  // Date -> seed hash (djb2 variant)
  private hashDateString(date: string): number {
    let hash = 5381;
    for (let i = 0; i < date.length; i++) {
      hash = (hash * 33) ^ date.charCodeAt(i);
    }
    return hash >>> 0;
  }

  // Deterministic PRNG (Mulberry32)
  private mulberry32(seed: number): () => number {
    let state = seed >>> 0;
    return () => {
      state = (state + 0x6d2b79f5) >>> 0;
      let t = state;
      t = Math.imul(t ^ (t >>> 15), t | 1);
      t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  // --- Helper Functions ---

  // Pre-filters dictionary to extract words with exactly 7 unique characters
  private extractPangrams(dictionary: string[]): string[] {
    const pangrams: string[] = [];
    for (let i = 0; i < dictionary.length; i++) {
      const word = dictionary[i];
      if (new Set(word).size === GAME_RULES.REQUIRED_LETTERS_COUNT) {
        pangrams.push(word);
      }
    }
    return pangrams;
  }

  // Checks if all characters in the word belong to the set of daily board letters
  private wordUsesOnlySet(word: string, letterSet: Set<string>): boolean {
    for (let i = 0; i < word.length; i++) {
      if (!letterSet.has(word[i])) return false;
    }
    return true;
  }

  /**
   * Builds the GameBoard object strictly compliant with the GameBoard interface, including cells, 
   * possible words, mielegrammi, and maxScore.
   */
  private buildGameBoard(
    date: string,
    seed: string,
    all7Letters: string[],
    centerLetter: string,
    possibleWords: string[],
    mielegrammi: string[]
  ): GameBoard {
    const outerLetters = all7Letters.filter((l) => l !== centerLetter);
    const mielegrammiSet = new Set(mielegrammi);

    const cells: Cell[] = [
      {
        id: 'hex-0',
        letter: centerLetter,
        position: 0 as HexPosition,
        isCenter: true,
      },
      ...outerLetters.map((letter, idx) => ({
        id: `hex-${idx + 1}`,
        letter,
        position: (idx + 1) as HexPosition,
        isCenter: false,
      })),
    ];

    // Score calculation
    const maxScore = possibleWords.reduce((sum, word) => {
      const basePoints = word.length === GAME_RULES.MIN_WORD_LENGTH ? 1 : word.length;
      const bonus = mielegrammiSet.has(word) ? GAME_RULES.MIELEGRAMMA_BONUS : 0;
      return sum + basePoints + bonus;
    }, 0);

    return {
      date,
      seed,
      cells,
      possibleWords,
      mielegrammi,
      maxScore,
    };
  }
}