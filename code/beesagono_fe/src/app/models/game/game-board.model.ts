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

  /** List of all valid target words for today's board (length >= MIN_WORD_LENGTH) */
  possibleWords: string[];

  /** Sub-list of target words using all 7 letters (Pangrams) */
  mielegrammi: string[];

  /** Maximum possible score for this board (sum of word points + mielegramma bonuses) */
  maxScore: number;
}