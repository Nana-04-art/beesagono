# Data Models & Core Algorithm Specification — Beesagono

This document freezes the TypeScript interfaces, the daily puzzle generation algorithm, and the storage/behavioral rules before implementation begins. It supersedes all previous drafts of `data-models.md` and complements `architecture.md`, `requirements.md`, `flowchart.md`, `ui-structure.md`, and `use-cases.md`.

---

## 1. Honeycomb Position Type (`HexPosition`)

Positions on the fixed 7-hexagon board grid:
- `0`: Mandatory Center Tile
- `1..6`: Outer Radial Tiles (Top, Top-Right, Bottom-Right, Bottom, Bottom-Left, Top-Left)

**File:** `src/app/models/hex-position.type.ts`

```typescript
export type HexPosition = 0 | 1 | 2 | 3 | 4 | 5 | 6;
```

---

## 2. Cell / Tile Model (`Cell`)

Represents an individual hexagon tile on the game UI canvas.

**File:** `src/app/models/cell.model.ts`

```typescript
import { HexPosition } from './hex-position.type';

export interface Cell {
  /** Unique ID for trackBy DOM operations (e.g., 'hex-0') */
  id: string;

  /** Uppercase letter displayed on the tile */
  letter: string;

  /** Spatial position index on the honeycomb board (0 = center). Shuffle (FR-05) only reorders positions 1-6; position 0 is immutable. */
  position: HexPosition;

  /** True if this is the central mandatory letter tile */
  isCenter: boolean;

  /** Visual state: true if currently highlighted during active input composition */
  isSelected?: boolean;

  /** Visual state: true while pressed/clicked (triggers shrink/bounce CSS) */
  isActive?: boolean;
}
```

---

## 3. Game Board Model (`GameBoard`)

Represents the physical layout, letter setup, and valid targets for today's puzzle.

**File:** `src/app/models/game-board.model.ts`

```typescript
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
```

---

## 4. Game State / Storage Model (`GameState`)

Structure saved to `localStorage` to persist player progress.

**File:** `src/app/models/game-state.model.ts`

```typescript
export interface GameState {
  /** Schema version — see Section 10.3 for migration rules */
  version: number; // current value: 1

  /** Date string key (YYYY-MM-DD) */
  date: string;

  /** Accumulated player points */
  score: number;

  /** List of words successfully found today */
  foundWords: string[];

  /** List of Mielegrammi (pangrams) found today */
  foundMielegrammi: string[];

  /** True if all possible target words have been found */
  isCompleted: boolean;

  /** Timestamps for stats tracking */
  startTime: number;
  lastUpdated: number;
}
```

> **Note on rank:** the player's rank is **not** persisted. It is a derived value, computed reactively via an Angular `computed()` signal from `score / GameBoard.maxScore`, mapped against the thresholds in Section 5. Persisting it would risk stale/frozen ranks if the threshold table changes in a future release.

---

## 5. Rank System

Ranks are computed client-side as `percentage = (score / maxScore) * 100`, rounded down, and mapped to the highest tier whose threshold is met. Always recalculated on read — never cached in storage.

| Threshold (%) | Rank Label |
| :--- | :--- |
| 0 | Iniziato |
| 2 | Mente Fresca |
| 5 | Principiante |
| 8 | Avanzato |
| 15 | Esperto |
| 25 | Eccellente |
| 40 | Genio |
| 70 | Maestro |
| 100 | Ape Regina |

```typescript
export interface RankTier {
  threshold: number; // minimum % (0-100) required to reach this rank
  label: string;
}
```

---

## 6. Validation Result Model (`ValidationResult`)

Result returned by `GameService.submitWord()`.

**File:** `src/app/models/validation.model.ts`

```typescript
/** Frozen, final set of validation error codes — must match toast copy exactly. */
export type ValidationErrorType =
  | 'TOO_SHORT'
  | 'MISSING_CENTER'
  | 'INVALID_LETTERS'
  | 'ALREADY_FOUND'
  | 'NOT_IN_DICTIONARY';

export interface ValidationResult {
  isValid: boolean;
  pointsAwarded: number;
  isMielegramma: boolean;
  errorType?: ValidationErrorType;
  message?: string;
}
```

---

## 7. Static Game Configuration (`GameRulesConfig`)

Central configuration constants for business rules — avoids magic numbers throughout the codebase.

**File:** `src/app/config/game-rules.config.ts`

```typescript
export const GAME_RULES = {
  MIN_WORD_LENGTH: 4,
  MIELEGRAMMA_BONUS: 7,
  REQUIRED_LETTERS_COUNT: 7,
  MIN_TARGET_WORDS_COUNT: 15,
  MIN_MIELEGRAMMI_COUNT: 1,
  MAX_GENERATION_ATTEMPTS: 500,
} as const;
```

---

## 8. Daily Puzzle Generation Algorithm

### 8.1 Determinism
The puzzle is fully deterministic: the **client's local date string** (`YYYY-MM-DD`) is used to derive a numeric seed for a seeded PRNG (e.g. Mulberry32 or equivalent lightweight implementation). Given the same date, the algorithm always produces the same puzzle — no server round-trip is required.

### 8.2 Generation Loop

```
1. seed = hash(dateString)
2. attempt = 0
3. LOOP (max GAME_RULES.MAX_GENERATION_ATTEMPTS iterations):
   a. rng = PRNG(seed + attempt)
   b. candidateLetters = pick REQUIRED_LETTERS_COUNT (7) unique letters
      from the dictionary's alphabet using rng, ensuring at least 1 vowel
      is included
   c. FOR EACH letter L in candidateLetters (as a candidate center):
        - compute targetWords(L) = filter dictionary where:
            * word.length >= GAME_RULES.MIN_WORD_LENGTH
            * word contains L at least once
            * every character in word belongs to candidateLetters
              (letter SET membership, not letter count — repeats allowed)
        - compute mielegrammi(L) = subset of targetWords(L) using all 7
          candidateLetters at least once each
        - IF targetWords(L).length >= MIN_TARGET_WORDS_COUNT
           AND mielegrammi(L).length >= MIN_MIELEGRAMMI_COUNT:
             mark L as a QUALIFYING center candidate
   d. IF at least one qualifying center candidate exists:
        - pick one among them via rng -> this is the center letter
        - ACCEPT this puzzle (candidateLetters + chosen center +
          corresponding targetWords/mielegrammi), STOP
      ELSE:
        - attempt += 1, GO TO step a (discard candidateLetters, re-seed)
4. IF loop exhausts MAX_GENERATION_ATTEMPTS without success:
   - log an error (malformed/too-small dictionary)
   - fall back to the last generated candidate regardless of Quality Gate,
     to guarantee the app never fails to render a board
```

### 8.3 Constraints Recap
- **Letter uniqueness:** the 7 board letters must be strictly unique — no duplicates.
- **Center letter:** chosen via the seeded PRNG, but constrained to letters that satisfy the Quality Gate (8.2.c/d) — not picked before validation.
- **Quality Gate:** `targetWords.length >= 15` and `mielegrammi.length >= 1`.
- **`maxScore`** is computed once at generation time as the sum of per-word points (Section 9) across all `possibleWords`, plus `MIELEGRAMMA_BONUS` (7) per entry in `mielegrammi`.

### 8.4 Word Validity Rule (clarifying FR-06 Rule 3)
A word is a valid target word if and only if:
1. Its length is ≥ `MIN_WORD_LENGTH` (4).
2. It contains the center letter at least once.
3. Every character in the word belongs to the set of 7 daily letters (repeated letters within a word are allowed, since letter *set* membership — not letter *count* — is what's validated).

---

## 9. Scoring Rules

- 4-letter word = **1 point**
- Word > 4 letters = **1 point per letter**
- Mielegramma (uses all 7 daily letters) = **+`GAME_RULES.MIELEGRAMMA_BONUS` (7) bonus points**, in addition to the base word score

---

## 10. Persistence Strategy (`StorageService`)

### 10.1 Storage Key Schema
- Game state: `beesagono:game:{YYYY-MM-DD}` → `GameState`
- (Optional, future) aggregate stats: `beesagono:stats` → separate object, not covered by this version.

### 10.2 Write/Read Flow
- Every mutation to in-memory game signals (found word, score update, completion) triggers an Angular `effect()` that serializes a `GameState` object and writes it via `StorageService.save()`.
- On app load, `StorageService.load(date)` attempts to read and parse the key for today's date. If absent, or if the stored `date` doesn't match today, a fresh game state is initialized (per UC-01) using a freshly generated `GameBoard`.

### 10.3 Schema Versioning
`GameState.version` starts at `1`. If a future release changes the persisted shape, `StorageService.load()` must check `version` and either migrate the object or discard it and start fresh — a mismatched-version object must never be passed directly into the live game state.

### 10.4 Failure Handling (Quota Exceeded / Blocked Storage)
All `localStorage` calls are wrapped in `try/catch` inside `StorageService`:
- **On write failure:** the service transparently falls back to an **in-memory store** (a plain object held for the lifetime of the page) so gameplay is unaffected within the session.
- **User feedback:** a non-blocking banner/toast is shown once per session:
  > "Attenzione: Impossibile salvare i progressi in locale. La sessione andrà persa alla chiusura della pagina."
- **On read failure:** treated identically to "no saved state found" — the app proceeds to initialize a fresh daily game rather than blocking.

---

## 11. Date-Change Detection (Midnight Rollover)

- **Timezone policy:** the player's **local browser date** is authoritative (no UTC normalization), prioritizing immediacy for the user over cross-timezone puzzle synchronization.
- **Detection mechanism:** the app listens for the `visibilitychange` event. When the tab/PWA regains foreground visibility, it compares `getCurrentLocalDateString()` against the currently loaded `GameState.date`.
- **On mismatch:**
  1. Show a toast: *"È iniziato un nuovo giorno! Caricamento del nuovo puzzle..."*
  2. Persist the current (previous-day) state as-is (no further writes to it).
  3. Generate and load the new daily `GameBoard` per Section 8, resetting game state (fresh `GameState` with `version: 1`, empty `foundWords`/`foundMielegrammi`, `score: 0`).

---

## 12. Input Handling Rules

- `handleInput(rawChar: string)` normalizes every character via `.toUpperCase()`.
- Non-alphabetic characters (numbers, symbols, whitespace) are silently discarded — not appended to `currentInput`.
- **Mobile keyboard:** the text input is set to `readonly` / `inputmode="none"` so the native virtual keyboard never appears. All input on touch devices comes exclusively through taps on the 7 SVG hexagons and the on-screen Delete/Enter controls.

---

## 13. Share Score Format

Formatted as Wordle-style plain text for clipboard copy, generated from current score/board data:

```
Beesagono 23/07/2026
Punteggio: 42/85 pts
Parole trovate: 12
Mielegrammi: 1/1
```

Date is rendered as `DD/MM/YYYY` to match Italian locale conventions.

```typescript
export interface ShareScorePayload {
  date: string;            // DD/MM/YYYY, display format
  score: number;
  maxScore: number;
  wordsFound: number;
  totalWords: number;
  mielegrammiFound: number;
  totalMielegrammi: number;
}
```

---

## 14. Loading State

While `dictionary.json` is being fetched/parsed and while the puzzle generation loop (Section 8) runs, the UI displays a full-screen spinner/overlay with the text:

> "Preparazione dell'alveare..."

The overlay is dismissed only after `GameBoard` and `GameState` are both fully initialized (either freshly generated or restored from storage).