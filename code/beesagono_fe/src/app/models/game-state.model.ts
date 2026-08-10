export interface GameState {
  /** Schema version */
  version: number; // current value: 1

  /** Date string key (YYYY-MM-DD) */
  date: string;

  /** Accumulated player points */
  score: number;

  /** List of words successfully found today */
  foundWords: string[];

  /** List of words found today who aren't in the valid word list */
  invalidWords?: string[];

  /** List of Mielegrammi (pangrams) found today */
  foundMielegrammi: string[];

  /** True if all possible target words have been found */
  isCompleted: boolean;

  /** Timestamps for stats tracking */
  startTime: number;
  lastUpdated: number;
}