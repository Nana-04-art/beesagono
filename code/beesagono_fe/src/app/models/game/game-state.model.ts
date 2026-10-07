export interface GameState {
  /** Schema version */
  version: number; // current value: 1

  /** Date string key (YYYY-MM-DD) */
  date: string;

  /** Accumulated player points */
  score: number;

  /** Center letter for the puzzle */
  centerLetter?: string;

  /** Outer letters for the puzzle */
  outerLetters?: string[];

  /** List of words successfully found today */
  foundWords: string[];

  /** List of words found today who aren't in the valid word list */
  invalidWords?: string[];

  /** List of Mielegrammi (pangrams) found today */
  foundMielegrammi: string[];

  /** Completion percentage of the puzzle (0.0 to 1.0) */
  completionPercentage?: number;

  /** True if all possible target words have been found */
  isCompleted: boolean;

  /** Timestamps for stats tracking */
  startTime: number;
  lastUpdated: number;

  /** Optional rank label for the puzzle */
  rankLabel?: string;
}