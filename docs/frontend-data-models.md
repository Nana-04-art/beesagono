# Frontend Data Models & Core Algorithm Specification — Beesagono

This document freezes the TypeScript interfaces, the daily puzzle generation algorithm, and the storage/behavioral rules. It complements `architecture.md`, `requirements.md`, `flowchart.md`, `ui-structure.md`, `use-cases.md`, and `service-component-contracts.md`.

---

## 1. Honeycomb Position Type (`HexPosition`)

**File:** `src/app/models/hex-position.type.ts`

```typescript
export type HexPosition = 0 | 1 | 2 | 3 | 4 | 5 | 6;
```

---

## 2. Cell / Tile Model (`Cell`)

**File:** `src/app/models/cell.model.ts`

```typescript
import { HexPosition } from './hex-position.type';

export interface Cell {
  id: string;
  letter: string;
  position: HexPosition;
  isCenter: boolean;
  isSelected?: boolean;
  isActive?: boolean;
}
```

---

## 3. Game Board Model (`GameBoard`) — **(UPDATED)**

**File:** `src/app/models/game-board.model.ts`

`centerLetter` / `outerLetters` / `availableLetters` are **no longer stored fields** on `GameBoard` (earlier drafts of this document listed them as `readonly` interface members). They are derived on demand via pure selector functions in `src/app/models/game-board.selectors.ts`, to keep `cells` the single source of truth and avoid any risk of the two going out of sync:

```typescript
export function getCenterLetter(board: GameBoard): string;   // board.cells.find(c => c.isCenter)!.letter
export function getOuterLetters(board: GameBoard): string[]; // cells.filter(c => !c.isCenter).map(c => c.letter)
export function getAvailableLetters(board: GameBoard): string[]; // cells.map(c => c.letter)
```

```typescript
import { Cell } from './cell.model';

export interface GameBoard {
  /** Daily puzzle date key in ISO format (YYYY-MM-DD) */
  date: string;

  /** Seed string used for PRNG deterministic generation, e.g. "<dateHash>_<attempt>" */
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
```

### 3a. `GameBoard` Selectors (`game-board.selectors.ts`)

Pure, side-effect-free functions — the only sanctioned way to read letters off a board. Never re-derive these inline elsewhere.

```typescript
export function getCenterLetter(board: GameBoard): string {
  return board.cells.find((c) => c.isCenter)!.letter;
}

export function getOuterLetters(board: GameBoard): string[] {
  return board.cells.filter((c) => !c.isCenter).map((c) => c.letter);
}

export function getAvailableLetters(board: GameBoard): string[] {
  return board.cells.map((c) => c.letter);
}
```

---

## 4. Game State / Storage Model (`GameState`) — **(UPDATED)**

**File:** `src/app/models/game-state.model.ts`

```typescript
export interface GameState {
  /** Schema version — see §12.3 for migration rules */
  version: number; // current value: 1

  /** Date string key (YYYY-MM-DD) */
  date: string;

  /**
   * The ONLY source-of-truth game-progress field. score, foundMielegrammi,
   * and isCompleted are NEVER persisted — they are always recomputed by
   * GameService as computed() signals from (foundWords + the current
   * GameBoard), so storage corruption or a stale version can never
   * desynchronize them from foundWords.
   */
  foundWords: string[];

  /** NEW: words attempted today that failed validation (excludes duplicates of already-found words) */
  invalidWords?: string[];

  /** List of Mielegrammi (pangrams) found today */
  foundMielegrammi: string[];

  /** True if all possible target words have been found */
  isCompleted: boolean;

  /** Timestamps for stats tracking */
  startTime: number;
  lastUpdated: number;

  /** NEW: snapshot of the rank label achieved as of the last save today */
  rankLabel?: string;
}
```

> **Note on rank persistence:** the player's **live** rank remains a derived value, recomputed reactively via `computed()` from `score / GameBoard.maxScore` against the thresholds in §5 — it is never read back from storage to drive the UI. `rankLabel` is written for **historical/statistics purposes only** (`StatsService` uses it to rebuild the daily rank-distribution histogram and season history when replaying past `GameState` entries — see §7 and §9).

---

## 5. Rank System — **(UPDATED: emoji labels)**

Ranks are computed client-side as `percentage = (score / maxScore) * 100`, rounded down, mapped to the highest tier whose threshold is met. Always recalculated on read, never cached.

| Threshold (%) | Rank Label |
| :--- | :--- |
| 0 | 🌱 Iniziato |
| 2 | 🍃 Mente Fresca |
| 5 | 🐣 Principiante |
| 8 | 🚀 Avanzato |
| 15 | 💡 Esperto |
| 25 | ⭐ Eccellente |
| 40 | 🧠 Genio |
| 70 | 👑 Maestro |
| 100 | 🐝 Ape Regina |

```typescript
export interface RankTier {
  threshold: number; // minimum % (0-100) required to reach this rank
  label: string;
}
```
This is the **daily** rank (per-puzzle). See §9 for the separate, longer-lived **career tier** system.

---

## 6. Validation Result Model (`ValidationResult`) — unchanged

**File:** `src/app/models/validation.model.ts`

```typescript
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

## 7. Static Game Configuration (`GameRulesConfig`) — unchanged

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

## 8. Daily Puzzle Generation Algorithm — **(UPDATED: Candidate Pangrams Strategy)**

### 8.1 Determinism
The puzzle is fully deterministic. Two algorithms are frozen exactly as implemented in `puzzle-generator.service.ts` (the canonical reference — any port to another language/runtime must match it bit-for-bit):

**Date → seed hash** (djb2 variant):
```typescript
function hashDateString(date: string): number {
  let hash = 5381;
  for (let i = 0; i < date.length; i++) {
    hash = (hash * 33) ^ date.charCodeAt(i);
  }
  return hash >>> 0;
}
```

**PRNG** (Mulberry32, exact bit operations):
```typescript
function mulberry32(seed: number): () => number {
  let state = seed >>> 0;
  return () => {
    state = (state + 0x6d2b79f5) >>> 0;
    let t = state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
```

**Dictionary pinning:** because letter weighting (§8.2.b) is derived from the dictionary's own content, `dictionary.json` must be treated as versioned input. Regenerating a *past* date's puzzle after editing `dictionary.json` may produce a different board. This is not a bug for normal play (only "today" is ever generated live), but any test vectors below are only valid against the exact `dictionary.json` snapshot they were computed from.

**Test vector:** To be verified against the current `dictionary.json` snapshot via the first unit test run (`puzzle-generator.service.spec.ts`).

| Input | Value |
| :--- | :--- |
| `date` | `"2026-07-27"` |
| seed (`hashDateString`) | `1385072001` |
| attempts before Quality Gate passed | `12` |
| resulting letters | `C, E, G, N, O, R, S` |
| center letter | `R` |
| target words count | `19` |
| mielegrammi | `["CONGRESSO"]` |

Any implementation must reproduce this exact result for this date against this dictionary snapshot. Add this as an automated unit test (`puzzle-generator.service.spec.ts`) before writing more logic on top of `PuzzleGeneratorService`.

### 8.2 Generation Loop (Candidate Pangrams Strategy)

```
1. seed = hashDateString(date)
2. candidates = extractPangrams(dictionary)
   // Pre-filters dictionary to words with exactly 7 unique letters (Set.size === 7)
3. attempt = 0
4. LOOP (max GAME_RULES.MAX_GENERATION_ATTEMPTS iterations):
   a. rng = PRNG(seed + attempt)
   b. targetPangram = candidates[ floor(rng() * candidates.length) ]
  // Deterministically pick today's candidate pangram
   c. uniqueLetters = Array.from( new Set(targetPangram) )
   d. centerLetter = uniqueLetters[ floor(rng() * 7) ]
  // Deterministically pick 1 mandatory center letter out of the 7
   e. compute targetWords = filter dictionary where:
   * word.length >= GAME_RULES.MIN_WORD_LENGTH (4)
   * word contains centerLetter at least once
   * every character in word belongs to uniqueLetters
   (letter SET membership, repeated characters allowed)
   f. compute mielegrammi = subset of targetWords using all 7 uniqueLetters
   g. IF targetWords.length >= GAME_RULES.MIN_TARGET_WORDS_COUNT (15)
      AND mielegrammi.length >= GAME_RULES.MIN_MIELEGRAMMI_COUNT (1):
        - ACCEPT this puzzle (uniqueLetters + centerLetter + targetWords + mielegrammi), STOP
      ELSE:
        - attempt += 1, GO TO step a (increment seed and test the next candidate pangram)
5. IF loop exhausts MAX_GENERATION_ATTEMPTS without success:
   - log an error and fall back to the last generated candidate regardless of Quality Gate, guaranteeing the app never fails to render a playable board.
```

### 8.3 Constraints Recap
- **Letter uniqueness:** the 7 board letters are strictly unique (guaranteed by deriving them from a word's unique-letter set).
- **Center letter:** chosen via the seeded PRNG from among the 7 candidate letters — not validated against the Quality Gate itself; only the resulting `targetWords`/`mielegrammi` are.
- **Quality Gate:** `targetWords.length >= 15` and `mielegrammi.length >= 1`.
- **`maxScore`** is computed once at generation time as the sum of per-word points (§10) across all `possibleWords`, plus `MIELEGRAMMA_BONUS` (7) per entry in `mielegrammi`.

### 8.4 Word Validity Rule (clarifying FR-06 Rule 3) — unchanged
A word is a valid target word iff: (1) length ≥ `MIN_WORD_LENGTH` (4), (2) contains the center letter at least once, (3) every character belongs to the set of 7 daily letters (repeats within a word are allowed — letter *set* membership is what's checked, not letter *count*).

---

## 9. Rank & Career-Tier Systems — **(NEW / clarified)**

### 9a. Daily Rank
See §5 above — computed per-puzzle from `score / GameBoard.maxScore`.

### 9b. Career Tiers (season-long, `CAREER_TIERS`)

**File:** `src/app/config/career-tiers.constant.ts`

A **separate, longer-lived** progression, independent of the daily rank, based on the ratio of a season's `totalSeasonPoints` (§9c) to an estimated max achievable score up to the current day of the year (`dayOfYear × 25`):

| minPercentage (%) | Career Tier |
| :--- | :--- |
| 0 | Uovo d'Ape |
| 15 | Larva |
| 30 | Ape Nutrice |
| 45 | Ape Operaia |
| 60 | Ape Bottinatrice |
| 75 | Ape Custode |
| 85 | Ape Guardiana |
| 95 | Ape Architetto |
| 100 | Ape Regina della Stagione |

```typescript
export interface CareerTier {
  name: string;
  minPercentage: number;
}
```

### 9c. Streak Milestone Bonuses (`STREAK_MILESTONES`)

**File:** `src/app/config/career-tiers.constant.ts` (co-located with `CAREER_TIERS`)

A one-time bonus (per season, per milestone) is added to `bonusStreakPoints` the first time the current consecutive-day streak reaches each of the following lengths:

| Streak length (days) | Bonus points |
| :--- | :--- |
| 3 | 50 |
| 7 | 150 |
| 15 | 350 |
| 30 | 800 |
| 50 | 1500 |
| 100 | 3500 |
| 200 | 8000 |
| 365 | 20000 |

```typescript
export const STREAK_MILESTONES: Record<number, number>;
```

---

## 10. Player Statistics Models (`stats.model.ts`) — **(NEW)**

**File:** `src/app/models/stats.model.ts`

```typescript
export interface SeasonStats {
  year: number;
  basePointsEarned: number;        // cumulative daily final scores this season
  bonusStreakPoints: number;       // cumulative one-time milestone bonuses this season
  totalSeasonPoints: number;       // basePointsEarned + bonusStreakPoints
  highestTierAchieved: string;     // CareerTier.name
  claimedStreakMilestones: number[]; // streak lengths already awarded this season
  // internal per-day bookkeeping, not meant for direct UI consumption:
  _lastRecordedDailyScore?: number;
  _isCompletedToday?: boolean;
  _lastRecordedRankToday?: string | null;
}

export interface PlayerStats {
  gamesPlayed: number;
  gamesCompleted: number;
  currentStreak: number;
  maxStreak: number;
  lastPlayedDate: string | null;               // YYYY-MM-DD
  currentSeason: SeasonStats;
  seasonHistory: Record<number, SeasonStats>;   // keyed by year, past seasons only
  dailyRankDistribution: Record<string, number>; // e.g. { 'Iniziato': 2, 'Genio': 5 }
}

/** Legacy/simplified shape, not currently populated by StatsService but kept as a documented model. */
export interface GameStats {
  gamesPlayed: number;
  gamesWon: number;
  currentStreak: number;
  maxStreak: number;
  lastPlayedDate: string;
  rankDistribution: Record<string, number>;
}
```

Persisted as a single blob under `beesagono:stats` (see §12). If missing, `StatsService` rebuilds it by scanning and chronologically replaying every stored `beesagono:game:*` entry that passes `isValidGameState` (§11).

---

## 11. Game State Validator (`game-state.validator.ts`) — **(NEW)**

**File:** `src/app/utils/game-state.validator.ts`

```typescript
/** Strict ISO 8601 (YYYY-MM-DD) check, including calendar correctness (month/day ranges, leap years) via a UTC round-trip. */
export function isValidIsoDate(dateStr: unknown): dateStr is string;

/**
 * Type guard used by StorageService/StatsService consumers before trusting a raw
 * (possibly stale-schema or corrupted) object as a GameState:
 * requires version === 1, a valid ISO date, a finite non-negative score,
 * a boolean isCompleted, foundWords/invalidWords as string arrays,
 * and rankLabel to be either omitted/null or a string.
 */
export function isValidGameState(obj: unknown): obj is GameState;
```
Used by `StatsService.rebuildStatsFromStorage()` to safely replay historical `beesagono:game:*` entries without crashing on legacy/corrupted shapes.

---

## 12. Persistence Strategy (`StorageService`) — **(UPDATED)**

### 12.1 Storage Key Schema
All keys are automatically namespaced with the `beesagono:` prefix by `StorageService.getFullKey()`:

| Logical key (as passed to `save`/`load`) | Full storage key | Payload |
| :--- | :--- | :--- |
| `game:{YYYY-MM-DD}` | `beesagono:game:{YYYY-MM-DD}` | `GameState` |
| `stats` | `beesagono:stats` | `PlayerStats` **(NEW — previously "optional, future")** |
| `user_theme` | `beesagono:user_theme` | `'light' \| 'dark'` **(NEW)** |
| `mielegrammi_welcome_disclaimer_seen` | `beesagono:mielegrammi_welcome_disclaimer_seen` | `boolean` **(NEW)** |

### 12.2 Write/Read Flow
- Every mutation to in-memory game signals (found/invalid word, score update, completion) triggers an Angular `effect()` in `GameService` that serializes a `GameState` (including `invalidWords` and a `rankLabel` snapshot) and writes it via `StorageService.save()`.
- On app load, `StorageService.load(key)` attempts to read/parse; a missing, unparsable, or version-mismatched result is treated identically to "no saved state" and a fresh `GameBoard`/`GameState` is initialized.
- `StorageService.save()` now also validates its own inputs up-front (non-empty key, non-`null`/`undefined` data), logging a `console.warn` and returning `false` without attempting a write if either is invalid.
- `StorageService.getKeysByPrefix(prefix)` enumerates matching full keys across both real `localStorage` and the in-memory fallback — used by `StatsService` to rebuild aggregate stats from individual daily game entries.

### 10.3 Schema Versioning
`GameState.version` starts at `1`. If a future release changes the persisted shape, `StorageService.load()` must check `version` and either migrate the object or discard it and start fresh — a mismatched-version object must never be passed directly into the live game state.

**Reconciliation on load (mandatory, beyond version-matching):** a matching `version` alone does not prove `foundWords` is still valid — the array must additionally be revalidated against the just-generated `GameBoard` before being applied to live signals:
1. Filter out any entry not present in `board.possibleWords` (defends against a corrupted/hand-edited storage entry or a dictionary change between sessions).
2. De-duplicate.
3. Only then recompute `score`, `foundMielegrammi`, and `isCompleted` from the cleaned list — never trust these values if they were ever persisted by an older client.

### 12.4 Failure Handling (Quota Exceeded / Blocked Storage)
All `localStorage` calls are wrapped in `try/catch` inside `StorageService`:
- **On write failure:** falls back to an in-memory `Map` held for the page's lifetime, so gameplay is unaffected within the session.
- **User feedback:** a non-blocking warning is available via `GameService.isStorageAvailable()` (see `ui-structure.md` §2.A for its current wiring status).
- **On read failure:** treated identically to "no saved state found."

---

## 13. Date-Change Detection (Midnight Rollover) — unchanged

- **Timezone policy:** the player's local browser date is authoritative (no UTC normalization).
- **Detection mechanism:** the app listens for both the `visibilitychange` event and `window:focus`, acting only when `document.visibilityState === 'visible'`, comparing the current local date against the loaded `GameBoard.date`.
- **On mismatch:** show a toast, persist the previous day's state as-is, then generate/load the new daily `GameBoard`, resetting game state (fresh `GameState`, `version: 1`, empty `foundWords`/`invalidWords`/`foundMielegrammi`, `score: 0`), and call `StatsService.recordGameStarted` for the new day.

---

## 14. Input Handling Rules — **(UPDATED)**

- `handleInput(rawChar: string)` normalizes every character via `.toUpperCase()`.
- Non-alphabetic characters are silently discarded.
- Two reserved tokens are also recognized: `'BACKSPACE'` → `deleteLastChar()`, `'ENTER'` → `submitWord()` — in addition to single A–Z letters, which are appended to `currentInput`. (The primary physical-keyboard listener in `HiveViewComponent` calls `deleteLastChar()`/`submitWord()` directly for those keys and only routes plain letters through `handleInput()`; the reserved tokens exist so any input source can use the same entry point.)
- **Mobile keyboard:** the text input is `readonly` / `inputmode="none"`; all touch input comes through the 7 SVG hexagons and the on-screen Delete/Enter controls.

---

## 15. Share Score Format — **(UPDATED delivery mechanism, unchanged payload/format)**

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
Formatted as Wordle-style plain text for clipboard copy:
```
🐝 Beesagono (23/07/2026)
Punti: 42/85
Parole: 12/30
Mielegrammi: 1/1
```
Delivered via a three-step fallback chain in `HiveViewComponent.shareResults()`: (1) `navigator.clipboard.writeText()`, (2) `navigator.share()` (Web Share API), (3) a legacy `document.execCommand('copy')` fallback via a hidden textarea — with a transient success/error toast for whichever step succeeds.

---

## 16. Loading State — **(UPDATED: explicit status signal + error/retry)**

`GameService.loadStatus()` is one of `'idle' | 'loading' | 'ready' | 'error'`:
- **`'loading'`** while `dictionary.json` is fetched/parsed and the puzzle generation loop (§8) runs — UI shows *"🐝 Preparazione dell'alveare..."*.
- **`'error'`** if the dictionary fetch/parse fails; `loadError()` carries a user-facing message and the UI offers a retry button calling `retryLoadDailyGame()`.
- **`'ready'`** once both `GameBoard` and `GameState` are fully initialized (freshly generated or restored) — the loading overlay is dismissed and the game renders.