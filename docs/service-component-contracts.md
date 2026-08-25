# Service & Component Public Contracts — Beesagono

This document freezes the **public API surface** of every service and standalone component: method signatures, exposed signals, and component Input/Output bindings. It complements `data-models.md`.

> This revision reflects the renamed/relocated files and newly-added services/components are called out explicitly.

All components and services target **Angular 21 signal-based APIs**: `signal()`, `computed()`, `input()` / `input.required()`, and `output()`.

---

## 1. Service Architecture Pattern

- **`GameService`** remains the single source of truth for **live gameplay** state (today's board, input, found/invalid words, score). It is injected directly by "smart" components that need reactive state — no prop-drilling of game state through the root component.
- Additional cross-cutting concerns are intentionally kept as **separate, single-responsibility services**, for the same reason as before — in-browser DI is a plain synchronous function call, so the split is purely for **testability and maintainability**, not performance:
  - **`ScoreService`** — scoring math and rank lookups (extracted out of `GameService` for isolated unit testing).
  - **`StatsService`** — career/season/streak aggregate statistics, which persist and evolve independently of any single day's `GameState`.
  - **`ThemeService`** — light/dark theme preference.
  - **`WelcomeNoticeService`** — first-launch disclaimer flag.
  - **`StorageService`**, **`DictionaryService`**, and **`PuzzleGeneratorService`** — unchanged rationale from the original design, used only by `GameService` (and now also directly by `StatsService`/`ThemeService`/`WelcomeNoticeService` for their own persistence).
- The presentational/smart split is **less strict** than originally frozen: `HeaderComponent` and `WordDisplayComponent` are now purely presentational, receiving state via `input()` from `HiveViewComponent` (which injects `GameService`); other newer components (`FoundWordsComponent`, `WordsByLetterComponent`, `InvalidWordsComponent`, `StatsComponent`, `RulesComponent`, `ScoreboardComponent`) inject `GameService` / `ScoreService` / `StatsService` directly where it simplifies wiring for nested popovers/panels.
- The root component is now **`HiveViewComponent`** (`src/app/pages/hive-view/`), lazy-loaded at the `/play` route — not a top-level `AppComponent` handling everything directly (see §11, Routing).

---

## 2. `GameService`

**File:** `src/app/services/game/game.service.ts`

### 2.1 Exposed Signals (readonly, public)

```typescript
readonly board: Signal<GameBoard | null>;  // null until loadDailyGame() resolves successfully
readonly loadStatus: Signal<'idle' | 'loading' | 'ready' | 'error'>;
readonly loadError: Signal<string | null>;
readonly currentInput: Signal<string>;
readonly foundWords: Signal<string[]>;
readonly foundMielegrammi: Signal<string[]>;      // computed
readonly invalidWords: Signal<string[]>;
readonly score: Signal<number>;                   // computed
readonly maxScore: Signal<number>;                // computed
readonly totalPossibleWords: Signal<number>;      // computed
readonly totalMielegrammi: Signal<number>;        // computed
readonly isCompleted: Signal<boolean>;            // computed
readonly isStorageAvailable: Signal<boolean>;

/** Derived, computed() from score / board().maxScore against the rank table (data-models.md §5), via ScoreService. Never persisted as a live source of truth. */
readonly rank: Signal<RankTier>;

/** Minimum points required to reach the current rank / unlock the next rank. */
readonly currentRankScore: Signal<number>;
readonly nextRankScore: Signal<number>;

/** True once score >= maxScore (top rank reached). */
readonly isQueenRank: Signal<boolean>;

/** Derived, computed() from board().cells with current shuffle order applied. */
readonly displayCells: Signal<Cell[]>;

/** All possible words for today, annotated with found/pangram flags, sorted by length then alphabetically. */
readonly wordMap: Signal<WordMapItem[]>;

/** Maps each of today's 7 letters to a palette color, for word-map rendering. */
readonly letterColors: Signal<Map<string, string>>;
```

### 2.2 Public Methods

```typescript
/** Records a "game started" stat entry for today via StatsService. */
initGame(): void;

/** Orchestrates dictionary load, deterministic puzzle generation, and storage restore. Sets loadStatus through 'loading' -> 'ready' | 'error'. */
loadDailyGame(): Promise<void>;

/** Re-attempts loadDailyGame() after a failure; no-op if loadStatus is 'loading' or 'ready'. */
retryLoadDailyGame(): Promise<void>;

/** Routes a single normalized character into currentInput. Backing implementation for FR-03's unified handler. */
handleInput(rawChar: string): void;

/** Removes the last character from currentInput. */
deleteLastChar(): void;

/** Clears currentInput entirely. */
clearInput(): void;

/** Runs the FR-06 validation pipeline; on failure (except duplicates) records the attempt into invalidWords; on success updates score/foundWords/persistence and notifies StatsService. */
submitWord(): ValidationResult;

/** Randomly reorders the 6 outer cell positions (1-6); position 0 is never touched. */
shuffle(): void;

/** Triggers UC-06 rollover if the local date has changed since the loaded puzzle's date. */
checkDateRollover(): void;

/** Builds the current ShareScorePayload from live state, for the Share Score feature. */
getShareScorePayload(): ShareScorePayload;

/** Looks up the RankTier for an arbitrary percentage (used internally by `rank`). */
getRankForPercentage(percentage: number): RankTier;
```

Notes:
- An internal Angular `effect()` auto-persists `GameState` (including `invalidWords` and a `rankLabel` snapshot) to `StorageService` under `game:${date}` whenever relevant signals change and `loadStatus() === 'ready'`.
- On successful `submitWord()`, `StatsService.recordProgress(date, newTotalScore, isCompleted, rank().label)` is called to keep season/streak stats in sync in real time (not just at day boundaries).

---

## 3. `StorageService`

**File:** `src/app/services/storage/storage.service.ts`

```typescript
/** Attempts to persist state; returns false (never throws) on an invalid key/undefined/null payload, or a write failure — triggering the in-memory fallback. */
save<T>(key: string, data: T): boolean;

/** Attempts to read and parse data for a key; returns null if absent, unreadable, or unparsable. */
load<T>(key: string): T | null;

/** Removes a stored entry for a given key (localStorage + in-memory fallback). */
clear(key: string): void;

/** True if the last read/write succeeded via real localStorage; false if running on the in-memory fallback. */
isAvailable(): boolean;

/** Returns every full (already-prefixed) key starting with the given prefix, merging localStorage and the in-memory fallback. */
getKeysByPrefix(prefix: string): string[];
```
All keys are transparently namespaced with the `beesagono:` prefix. Current key usage: `game:{YYYY-MM-DD}` (`GameState`), `stats` (`PlayerStats`), `user_theme` (`Theme`), `mielegrammi_welcome_disclaimer_seen` (`boolean`).

---

## 4. `DictionaryService`

**File:** `src/app/services/dictionary/dictionary.service.ts`

```typescript
/** Fetches dictionary.json once (array or { words: [...] } shape), sanitizes each entry (trim + uppercase, must match /^[A-Z]+$/), de-duplicates, and caches the result. Throws a descriptive error on HTTP failure, invalid shape, or zero valid words. */
loadDictionary(): Promise<string[]>;

/** Returns the O(1)-lookup Set built from the loaded word list. Throws if called before loadDictionary() resolves. */
getWordSet(): Set<string>;
```

---

## 5. `PuzzleGeneratorService`

**File:** `src/app/services/puzzle-generator/puzzle-generator.service.ts`

```typescript
/** Pure function (no side effects): runs the Candidate Pangrams Strategy + Quality Gate loop from data-models.md §8 and returns a fully-formed GameBoard for the given date and dictionary. Safely handles an empty/undefined dictionary via a hardcoded fallback pangram. */
generateDailyPuzzle(date: string, wordSet: Set<string> | string[]): GameBoard;
```

---

## 6. `ScoreService` (NEW)

**File:** `src/app/services/score/score.service.ts`

```typescript
readonly dailyScore: Signal<number>;

calculateWordPoints(word: string, isMielegramma: boolean): number;
calculateTotalScore(foundWords: string[], mielegrammiSet: Set<string>): number;
getRankForPercentage(percentage: number): RankTier;
calculatePercentage(currentScore: number, maxScore: number): number;
getCurrentRank(currentScore: number, maxScore: number): RankTier;
getNextRank(score: number, max: number): RankTier | undefined;
getPointsToNext(score: number, max: number): number;
setDailyScore(score: number): void;
resetDailyScore(): void;
```

---

## 7. `StatsService` (NEW)

**File:** `src/app/services/stats/stats.service.ts`

```typescript
readonly stats: Signal<PlayerStats>;
readonly currentTier: Signal<string>;

/** Convenience wrapper: recordProgress(currentDate, 0, false, null). */
recordGameStarted(currentDate: string): void;

/**
 * Records incremental progress.
 * @param currentDate    YYYY-MM-DD
 * @param dailyScore     TOTAL score accumulated today (monotonically increasing)
 * @param isCompletedToday True once today's board is 100% complete
 * @param dailyRank      Today's currently-achieved rank label, or null to skip updating the distribution
 */
recordProgress(currentDate: string, dailyScore: number, isCompletedToday: boolean, dailyRank: string | null): void;
```
Handles: streak increment/reset on first play of a new day, calendar-year season rollover, cumulative base/bonus/total season points, one-time-per-season streak-milestone bonuses, completed-games counter, per-rank daily distribution histogram, and career-tier recalculation. On first construction, if no `beesagono:stats` blob exists, stats are rebuilt from scratch by scanning `beesagono:game:*` via `StorageService.getKeysByPrefix` and replaying entries that pass `isValidGameState` (`game-state.validator.ts`), chronologically.

---

## 8. `ThemeService` (NEW)

**File:** `src/app/services/theme/theme.service.ts`

```typescript
readonly currentTheme: Signal<'light' | 'dark'>;
toggleTheme(): void;
```
Reads/writes `beesagono:user_theme`; falls back to `window.matchMedia('(prefers-color-scheme: dark)')` when unset; an internal `effect()` applies `data-theme` on `document.documentElement` and persists on every change.

---

## 9. `WelcomeNoticeService` (NEW)

**File:** `src/app/services/welcome-notice/welcome-notice.service.ts`

```typescript
readonly isNoticeOpen: Signal<boolean>;

/** Opens the notice if the "seen" flag is missing, or fails open (opens the notice) if storage throws (e.g. SecurityError). */
checkAndShowNotice(): void;

/** Persists the "seen" flag (swallowing write errors) and always closes the notice. */
dismissNotice(): void;
```

---

## 10. Component Contracts

### `HiveViewComponent` (root page, replaces `AppComponent`)
- **File:** `src/app/pages/hive-view/hive-view.component.ts`, routed at `/play` (§11).
- **Inputs/Outputs:** none (root-of-tree component for its route).
- Injects `GameService`, `WelcomeNoticeService`.
- Hosts global `window:keydown`, `window:focus`, and `document:visibilitychange` HostListeners; delegates letter/Backspace/Enter to `GameService`, and calls `checkDateRollover()` on regained visibility.
- Owns UI-only local state not worth lifting into `GameService`: `isEndGameModalOpen`, `isHelpModalOpen`, `isStatsModalOpen`, and the transient `feedbackMessage` / `feedbackType` toast pair (populated by `submit()` and `shareResults()`).
- Renders the loading overlay while `loadStatus() === 'loading'`, an error+retry panel while `'error'`, and the full gameplay layout once `'ready'`.

### `HeaderComponent` (updated — now presentational)
- **Inputs:**
  ```typescript
  score = input<number>(0);
  rank = input<{ label: string }>({ label: '🌱 Iniziato' });
  formattedDate = input<string>('');
  ```
- **Outputs:**
  ```typescript
  statsRequested = output<void>();   // declared; not currently emitted/bound (Stats opens via the header's own popover instead) — kept for future wiring
  shareRequested = output<void>();
  logoClicked = output<void>();      // declared; not currently bound by HiveViewComponent
  ```
- Injects `ThemeService` directly (for the light/dark toggle button).
- Owns `activePopover = signal<'scoreboard' | 'rules' | 'stats' | null>(null)`; `toggleScoreboard()` / `toggleRules()` / `toggleStats()` / `closeAll()`; `@HostListener('document:keydown.escape') handleEscape()` closes all popovers.
- *(Gap vs. original design: `GameService.isStorageAvailable` is not currently consumed here for a storage-unavailable banner.)*

### `ScoreboardComponent` (NEW)
- **Path:** `src/app/components/header/scoreboard/scoreboard.component.ts`.
- **Inputs (optional; fall back to injected `GameService` if omitted):**
  ```typescript
  scoreInput = input<number | undefined>(undefined, { alias: 'score' });
  rankNameInput = input<string | undefined>(undefined, { alias: 'rankName' });
  ```
- **Outputs:** none.
- Injects `GameService`, `ScoreService`. Computes `progressPercentage`, `nextRankTier`, `pointsToNextRank`, `currentRankEmoji`/`nextRankEmoji` (first token of the rank label), `nextRankThreshold`, and `rankTiersWithPoints` (full `RANK_TIERS` annotated with required points / unlocked status).

### `RulesComponent` (NEW)
- **Path:** `src/app/components/header/rules/rules.component.ts`.
- **Inputs/Outputs:** none.
- Exposes `rankTiers = RANK_TIERS` for the template; renders static rules, scoring table, streak/season explainer, and the rank-tier table.

### `StatsComponent` (NEW)
- **Path:** `src/app/components/header/stats/stats.component.ts`.
- **Inputs/Outputs:** none.
- Injects `StatsService`; exposes `stats`, `currentTier`, and a computed `completionRate` (`gamesCompleted / gamesPlayed`, rounded %).

### `WordDisplayComponent` (updated — now fully presentational)
- **Inputs:**
  ```typescript
  currentInput = input.required<string>();
  centerLetter = input<string>('');
  feedbackMessage = input<string | null>(null);
  feedbackType = input<'error' | 'success' | 'info' | null>(null);
  ```
- **Outputs:** none.
- No service injection; `HiveViewComponent` owns and passes `feedbackMessage`/`feedbackType` down after each `submitWord()` / `shareResults()` call — this supersedes the original contract's "injects `GameService` directly" description.

### `HoneycombGridComponent`
- **Inputs:**
  ```typescript
  cells = input.required<Cell[]>();
  ```
- **Outputs:**
  ```typescript
  letterTapped = output<string>();
  ```
- Adds a pure `getHexCoordinates(position): { x: number; y: number }` helper (center at (160,160), outer radius 92, −90° start, 60° step) and keyboard activation (`onKeyDown`, Enter/Space) alongside `onCellClick`; both blur the tile afterward to prevent re-triggering on a held Enter key.

### `HiveControlsComponent` (renamed from `ControlsComponent`)
- **Inputs:** none.
- **Outputs:**
  ```typescript
  deletePressed = output<void>();
  shufflePressed = output<void>();
  submitPressed = output<void>();
  ```
- Adds an internal `isSubmitting` boolean guard with a ~200ms timeout to ignore rapid double activation of Submit.

### `FoundWordsComponent` (renamed from `FoundWordsPanelComponent`)
- **Inputs:**
  ```typescript
  foundWords = input<string[]>([]);
  foundMielegrammi = input<string[]>([]);
  totalPossibleWords = input<number>(0);
  totalPossibleMielegrammi = input<number>(0);
  ```
- **Outputs:** none.
- Also injects `GameService` directly, for `wordMap()` / `letterColors()`, needed to feed the nested `WordMapComponent` when the map view toggle is on — a hybrid of presentational inputs and direct injection.
- Local state: `isExpanded` (default **false**), `showMap` (default **false**).

### `WordsByLetterComponent` (NEW)
- **Inputs/Outputs:** none.
- Injects `GameService`.
- `letterGroups = computed<LetterGroup[]>()`: one entry per board cell (letter, `isCenter`, found/total word count, found/total Mielegramma count, matching `WordMapItem[]` slice).
- `expandedLetters` / `showMapLetters` signal sets, per-letter; all letters auto-expand on first board load via a one-shot `effect()` (`isInitialized` guard) without overwriting later manual toggles.
- `toggleExpand(letter)`, `toggleMapView(letter, event)`, `isMielegramma(word)`.

### `WordMapComponent` (NEW)
- **Inputs:**
  ```typescript
  items = input.required<WordMapItem[]>();
  letterColors = input.required<Map<string, string>>();
  ```
- **Outputs:** none. Purely presentational dot-grid renderer.

### `InvalidWordsComponent` (NEW)
- **Inputs/Outputs:** none.
- Injects `GameService` for `invalidWords`.
- `isExpanded` local signal, default **true** (opposite default from `FoundWordsComponent`).
- `toggleExpand()`.

### `WelcomeModalComponent` (NEW)
- **Inputs/Outputs:** none.
- Injects `WelcomeNoticeService`.
- `constructor()` `effect()`: toggles `document.body.style.overflow` and focuses the confirm button while the notice is open.
- `@HostListener('document:keydown')`: traps Tab (always re-focuses the single confirm button) and Escape (calls `dismissNotice()`).
- `dismissNotice()`: restores `body.style.overflow` and calls `WelcomeNoticeService.dismissNotice()`.

### `EndGameModalComponent`
- **Inputs:**
  ```typescript
  isOpen = input<boolean>(false);
  payload = input<ShareScorePayload | null>(null);   // now nullable, vs. the original input.required<ShareScorePayload>()
  ```
- **Outputs:**
  ```typescript
  closed = output<void>();
  shareRequested = output<void>();
  ```
- Full focus-trap implementation: saves/restores the previously-focused element, focuses the Share button on open (`setTimeout(0)`), and cycles Tab/Shift+Tab within the dialog's focusable elements (`@HostListener('document:keydown.tab')`); `@HostListener('document:keydown.escape')` closes.

---

## 11. Routing

**File:** `src/app/app.routes.ts`

```typescript
export const routes: Routes = [
  { path: '', redirectTo: 'play', pathMatch: 'full' },
  {
    path: 'play',
    loadComponent: () =>
      import('./pages/hive-view/hive-view.component').then(m => m.HiveViewComponent),
    title: 'Beesagono - Il Gioco del Miele',
  },
  { path: '**', redirectTo: 'play' },
];
```

---

## 12. Wiring Summary

```
HiveViewComponent (route: /play)
 ├─ app-welcome-modal          (WelcomeNoticeService — shown once, first launch)
 ├─ app-header                 (in: score/rank/formattedDate | out: shareRequested)
 │   ├─ app-scoreboard         (popover: rank progress)
 │   ├─ app-rules              (popover: rules & scoring)
 │   └─ app-stats              (popover: career/season stats, StatsService)
 ├─ app-word-display           (in: currentInput, centerLetter, feedbackMessage, feedbackType)
 ├─ app-honeycomb-grid         (in: displayCells | out: letterTapped → GameService.handleInput)
 ├─ app-hive-controls          (out: deletePressed/shufflePressed/submitPressed → GameService methods)
 ├─ app-found-words            (in: foundWords/foundMielegrammi/totals | injects GameService for word map)
 ├─ app-words-by-letter        (injects GameService)
 ├─ app-invalid-words          (injects GameService)
 └─ app-end-game-modal         (in: getShareScorePayload() | out: shareRequested → clipboard/Web Share/execCommand)
```