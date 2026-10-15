import { Cell } from './cell.model';

export interface GameBoard {
  /** Daily puzzle date key in ISO format (YYYY-MM-DD) */
  date: string;

  /** Seed string used for PRNG deterministic generation */
  seed: string;

  /**
   * Array containing exactly 7 cells (index 0 = center).
   * SINGLE SOURCE OF TRUTH for board letters and layout.
   * Shuffle (FR-05) mutates only `position` on entries 1-6; `letter` and
   * `isCenter` are never reassigned after initial generation.
   */
  cells: Cell[];

  // NOTE: centerLetter / outerLetters / availableLetters are intentionally
  // NOT stored here. They are cheap to derive from `cells` and storing them
  // separately would duplicate the single source of truth. See the pure
  // selector functions in `game-board.selectors.ts`, Section 3a below.

  /** Maximum possible score for this board (sum of word points + mielegramma bonuses) */
  maxScore: number;

  /** Total number of valid words for this board */
  totalWordsCount: number;

  /** Total number of valid mielegrammi for this board */
  totalMielegrammiCount: number;

  /** Number of valid words found for each letter (A-Z) */
  wordsCountByLetter: Record<string, number>;

  /** Number of valid mielegrammi found for each letter (A-Z) */
  mielegrammiCountByLetter: Record<string, number>;
}