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

  /**
   * DERIVED / READ-ONLY CACHE — computed once at puzzle generation time
   * from `cells`, and never mutated independently afterward (including
   * during shuffle). Do not write to these fields directly; regenerate
   * them only if `cells` itself is rebuilt from scratch.
   */
  readonly centerLetter: string;
  readonly outerLetters: string[];
  readonly availableLetters: string[];

  /** List of all valid target words for today's board (length >= MIN_WORD_LENGTH) */
  possibleWords: string[];

  /** Sub-list of target words using all 7 letters (Pangrams) */
  mielegrammi: string[];

  /** Maximum possible score for this board (sum of word points + mielegramma bonuses) */
  maxScore: number;
}

export interface RankTier {
  threshold: number; // minimum % (0-100) required to reach this rank
  label: string;
}