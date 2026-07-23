# Service & Component Public Contracts — Beesagono

This document freezes the **public API surface** of every service and standalone component: method signatures, exposed signals, and component Input/Output bindings. No implementation logic is included — this is the contract to code against, not the code itself. It complements `data-models.md` (which defines the types referenced here).

All components and services target **Angular 21 signal-based APIs**: `signal()`, `computed()`, `input()` / `input.required()`, and `output()`.

---

## 1. Service Architecture Pattern

- **`GameService`** is the single source of truth for live gameplay state. It is injected directly by "smart" components that need reactive state (`HeaderComponent`, `WordDisplayComponent`, `HoneycombGridComponent`, `FoundWordsPanelComponent`) — no prop-drilling of game state through `AppComponent`.
- **`ControlsComponent`** is presentational: it has no service dependency, only emits intent via `output()` signals, and `AppComponent`/`GameService` reacts to it. This keeps the button component trivially testable in isolation.
- **`StorageService`**, **`DictionaryService`**, and **`PuzzleGeneratorService`** are kept as separate, single-responsibility services and are used only by `GameService`; no component talks to them directly.
  - **Why separate, not merged into `GameService`:** in the browser, dependency-injected singleton services are plain synchronous function calls in memory — there is no network/IPC cost to calling `this.dictionaryService.getWordSet()` versus having that logic inlined. The only genuinely asynchronous operation in the whole system is the `fetch()` of `dictionary.json`, and that stays async regardless of which file the code lives in. The separation exists purely for **testability and maintainability** (each concern can be unit-tested and swapped in isolation — e.g. replacing `localStorage` with IndexedDB later touches only `StorageService`), not for performance.

---

## 2. `GameService`

**File:** `src/app/services/game.service.ts`

### 2.1 Exposed Signals (readonly, public)

```typescript
readonly board: Signal<GameBoard>;
readonly currentInput: Signal<string>;
readonly foundWords: Signal<string[]>;
readonly foundMielegrammi: Signal<string[]>;
readonly score: Signal<number>;
readonly isCompleted: Signal<boolean>;
readonly isStorageAvailable: Signal<boolean>;

/** Derived, computed() from score / board().maxScore against the rank table in data-models.md §5. Never persisted. */
readonly rank: Signal<RankTier>;

/** Derived, computed() from board().cells with current shuffle order applied. */
readonly displayCells: Signal<Cell[]>;
```

### 2.2 Public Methods

```typescript
/** Orchestrates dictionary load, deterministic puzzle generation, and storage restore. Called once on app bootstrap. */
loadDailyGame(): Promise<void>;

/** Routes a single normalized character into currentInput. Backing implementation for FR-03's unified handler. */
handleInput(rawChar: string): void;

/** Removes the last character from currentInput. */
deleteLastChar(): void;

/** Clears currentInput entirely (called after a successful submission). */
clearInput(): void;

/** Runs the FR-06 validation pipeline against currentInput and, if valid, updates score/foundWords/persistence. */
submitWord(): ValidationResult;

/** Randomly reorders the 6 outer cell positions (1-6); position 0 is never touched. */
shuffle(): void;

/** Called on the app's visibilitychange listener; triggers UC-06 rollover if the local date has changed. */
checkDateRollover(): void;

/** Builds the current ShareScorePayload from live state, for the Share Score feature. */
getShareScorePayload(): ShareScorePayload;
```

---

## 3. `StorageService`

**File:** `src/app/services/storage.service.ts`

```typescript
/** Attempts to persist state; returns false (never throws) on failure, triggering in-memory fallback in GameService. */
save(state: GameState): boolean;

/** Attempts to read state for a given date; returns null if absent, unreadable, or version-mismatched. */
load(date: string): GameState | null;

/** Removes a stored entry for a given date (used when rolling over to a new day). */
clear(date: string): void;

/** True if the last read/write succeeded via real localStorage; false if the service is running on the in-memory fallback. */
isAvailable(): boolean;
```

---

## 4. `DictionaryService`

**File:** `src/app/services/dictionary.service.ts`

```typescript
/** Fetches and parses dictionary.json once; cached for subsequent calls. */
loadDictionary(): Promise<string[]>;

/** Returns the O(1)-lookup Set built from the loaded word list. Throws if called before loadDictionary() resolves. */
getWordSet(): Set<string>;
```

---

## 5. `PuzzleGeneratorService`

**File:** `src/app/services/puzzle-generator.service.ts`

```typescript
/** Pure function (no side effects): runs the seeded-PRNG + Quality Gate loop from data-models.md §8 and returns a fully-formed GameBoard for the given date and dictionary. */
generateDailyPuzzle(date: string, wordSet: Set<string>): GameBoard;
```

---

## 6. Component Contracts

### `AppComponent`
- **Inputs:** none (root component).
- **Outputs:** none.
- **Responsibilities:** hosts global `keydown` and `visibilitychange` HostListeners, delegates to `GameService.handleInput()` / `GameService.checkDateRollover()`; renders the loading overlay while `GameService.loadDailyGame()` is pending.

### `HeaderComponent`
- **Inputs:** none (injects `GameService` directly for `score`, `rank`, `isStorageAvailable`).
- **Outputs:**
  ```typescript
  infoRequested = output<void>();  // opens the rules/info modal
  ```

### `WordDisplayComponent`
- **Inputs:** none (injects `GameService` directly for `currentInput`).
- **Outputs:** none.
- **Responsibilities:** renders `currentInput`, and reacts to `ValidationResult.errorType` (via a service-exposed signal or an `input()` set by the parent after `submitWord()`) to trigger the shake animation and toast copy from `ui-structure.md` §3.B.

### `HoneycombGridComponent`
- **Inputs:**
  ```typescript
  cells = input.required<Cell[]>();       // GameService.displayCells()
  ```
- **Outputs:**
  ```typescript
  letterTapped = output<string>();        // tapped hexagon's letter, forwarded to GameService.handleInput()
  ```

### `ControlsComponent`
- **Inputs:** none (fully presentational, stateless).
- **Outputs:**
  ```typescript
  deletePressed = output<void>();
  shufflePressed = output<void>();
  submitPressed = output<void>();
  ```

### `FoundWordsPanelComponent`
- **Inputs:** none (injects `GameService` directly for `foundWords`, `foundMielegrammi`).
- **Outputs:** none.

### `EndGameModalComponent`
- **Inputs:**
  ```typescript
  payload = input.required<ShareScorePayload>();
  isOpen = input<boolean>(false);
  ```
- **Outputs:**
  ```typescript
  shareRequested = output<void>();   // triggers clipboard copy of the formatted text
  closed = output<void>();
  ```

---

## 7. Wiring Summary

```
AppComponent
 ├─ HeaderComponent           (reads: score, rank, isStorageAvailable)
 ├─ WordDisplayComponent      (reads: currentInput)
 ├─ HoneycombGridComponent    (in: displayCells | out: letterTapped → GameService.handleInput)
 ├─ ControlsComponent         (out: deletePressed/shufflePressed/submitPressed → GameService methods)
 ├─ FoundWordsPanelComponent  (reads: foundWords, foundMielegrammi)
 └─ EndGameModalComponent     (in: getShareScorePayload() | out: shareRequested → clipboard)
```