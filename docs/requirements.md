# Functional & Non-Functional Requirements — Beesagono

> See `data-models.md` for TypeScript interfaces, the full puzzle generation algorithm, rank/career thresholds, and storage schema referenced throughout this document.

## 1. Functional Requirements (FR)

### Grid & Visual Representation
- **FR-01 (7-Hexagon Honeycomb Layout):** The board must render a 7-hexagon honeycomb grid: 1 central mandatory hexagon and 6 surrounding outer hexagons.
- **FR-02 (Hexagon Visual States):**
  - **Center Hexagon:** Highlighted with a distinct golden/amber theme to denote mandatory usage.
  - **Outer Hexagon:** Standard tile styling with active/hover animation states.
  - **Mielegramma (Pangram):** Gold celebration effects triggered when a word using all 7 daily letters is discovered.

### User Input Management
- **FR-03 (Unified Input Handling):** `GameService.handleInput(rawChar)` is the single entry point for character input.
  - Every character is normalized to uppercase (`.toUpperCase()`).
  - Non-alphabetic characters (numbers, symbols, whitespace) are silently discarded, never appended to `currentInput`.
  - Two reserved tokens, `'BACKSPACE'` and `'ENTER'`, are also recognized and routed to `deleteLastChar()` / `submitWord()` respectively, so alternate input sources can reuse the same handler. In the shipped UI, the physical-keyboard listener (`HiveViewComponent`) calls `deleteLastChar()` / `submitWord()` directly for the Backspace/Enter keys and only routes plain letters through `handleInput()`; honeycomb tile taps and typed letters both funnel through it.
  - On mobile/touch devices, the native virtual keyboard is disabled (`readonly` / `inputmode="none"` on the input field); input is exclusively via the on-screen hexagons and controls.
- **FR-04 (Action Controls):** `HiveControlsComponent` (formerly `ControlsComponent`) provides dedicated Delete/Shuffle/Submit handlers and adds an internal ~200ms re-entrancy guard on Submit to ignore accidental rapid double taps/clicks.
- **FR-05 (Shuffle Mechanism):** Shuffling must re-organize the 6 outer letters visually on the grid while keeping the mandatory center letter fixed.

### Daily Game Specifications
- **FR-ALG-01 (Midnight Puzzle Renewal):** At midnight (00:00, **local browser timezone**) or on first launch of the day, the system clears the previous session and generates a new daily puzzle. Detection happens via both the `visibilitychange` event and a `window:focus` listener (acting only when `document.visibilityState === 'visible'`): the current local date is compared against the loaded puzzle's date, and renewal is triggered on mismatch, with a toast shown before swapping puzzles.
- **FR-ALG-02 (Pre-Calculated Daily Sets):** Upon generation, `GameService` extracts `targetWords` (all valid dictionary words containing the center letter, using only the 7 daily letters, length ≥ 4) and `mielegrammi` (the subset using all 7 unique daily letters).
- **FR-ALG-03 (Zero-Lag In-Memory Validation):** `targetWords`/`mielegrammi` are held in in-memory `Set`s for instant O(1) validation.
- **FR-ALG-04 (Deterministic Generation — Candidate Pangrams Strategy):** The generation algorithm differs from earlier drafts of this document, which described direct weighted letter-frequency sampling of 7 letters. `PuzzleGeneratorService` now:
  1. Pre-extracts every dictionary word with exactly 7 unique letters ("pangram candidates"); if the dictionary yields none, falls back to a hardcoded seed word (`ALBERGO`) so the app can never fail to render a board.
  2. Seeds a Mulberry32 PRNG from a djb2 hash of the local date string (`YYYY-MM-DD`).
  3. On each attempt, deterministically (via the seeded RNG) picks one candidate pangram, derives its 7 unique letters (sorted alphabetically for tie-break determinism), and picks one of those 7 letters as the center.
  4. Computes `targetWords`/`mielegrammi` for that 7-letter set + center letter and checks the FR-ALG-05 quality gate.
  5. Retries with the next RNG draw up to `MAX_GENERATION_ATTEMPTS` (500); the last generated candidate is used as a guaranteed fallback if every attempt fails the gate.
  - Given the same date and dictionary, generation remains fully deterministic with no server round-trip.
- **FR-ALG-05 (Puzzle Quality Gate):** A candidate is only accepted if it yields ≥ 15 target words and ≥ 1 Mielegramma.

### Word Validation Rules
- **FR-06 (Validation Constraints):** Upon submission, check in sequence: (1) minimum length ≥ 4, (2) contains the center letter, (3) uses only the 7 daily letters, (4) not already in `foundWords`, (5) exists in `targetWords`.
- **FR-06a (Invalid Word Tracking):** Any submission failing rules 1–3 or 5 is appended (de-duplicated) to an `invalidWords` list, surfaced in a dedicated collapsible panel (`InvalidWordsComponent`) and persisted alongside the day's `GameState`. A duplicate of an already-found word (rule 4) is **not** added to `invalidWords`, since it was previously valid.
- **FR-07 (Error Feedback):** Display specific error toasts alongside a visual shake animation on the input field.

### Scoring & Game Progress
- **FR-08 (Scoring Engine):**
  - 4-letter words = 1 point.
  - Words > 4 letters = 1 point per letter.
  - **Mielegramma Bonus:** +7 bonus points awarded when all 7 distinct letters are used.
- **FR-09 (Discovered Words List):** Display a scrollable list of correctly found words and highlight discovered Mielegrammi.
- **FR-10 (Local Persistence):** Automatically persist current date and found words array in `localStorage` keyed by today's date string (`beesagono:game:{YYYY-MM-DD}`). Upon read, types are validated, words are sanitized against today's target words, and derived score/status are recomputed.
- **FR-10a (Persistence Failure Fallback):** If `localStorage` is unavailable or blocked (e.g. Safari private browsing, quota exceeded), the app must fall back transparently to an in-memory store for the current session and display a non-blocking warning that progress will not survive a page reload.
- **FR-11 (End Game Screen):** Display a completion summary modal when all target words are found, showing final statistics and a share score feature.
- **FR-11a (Share Score Format):** The share feature copies a Wordle-style plain-text summary to the clipboard, including the date (`DD/MM/YYYY`), score/max score, words found, and Mielegrammi found/total.
- **FR-12 (Rank Progression):** The player's rank is derived (never persisted) from `score / maxScore` percentage and displayed reactively in the header. See rank threshold table in `data-models.md`, Section 5.

---

## 2. Non-Functional Requirements (NFR)
- **NFR-01 (Architecture):** Angular 21 with Standalone Components and Signals for state management (`wordInput`, `foundWords`, `score`).
- **NFR-02 (Accessibility - Testable Specifications):**
  - **Interactive Tiles Semantics:** SVG hexagon tiles must expose `role="button"`, `tabindex="0"`, and dynamic `aria-label` attributes (e.g., `aria-label="Lettera centrale A"` for center tile, `aria-label="Lettera B"` for outer tiles).
  - **Keyboard Navigation:** Action controls and hexagon tiles must trigger on `Enter` and `Space` keypresses when focused.
  - **Visible Focus Indicator:** All focusable elements must display a high-contrast focus ring (`outline: 2px solid var(--focus-ring)`) meeting WCAG 2.1 SC 2.4.7.
  - **Live Region Announcements:** Error/success messages, score updates, and toasts must be wrapped in an `aria-live="polite"` container (`aria-live="assertive"` for critical load errors) to announce feedback immediately to screen readers.
  - **Modal Dialog Focus Management:** When a modal opens (`EndGameModalComponent`), focus must automatically move to the first interactive element and be trapped within the dialog (`focus trap`). Closing the modal restores focus to the triggering element.
  - **Global Listener Guards:** The document-level physical key listener must ignore key events (`keyup`/`keydown`) whenever `event.target` is an input, textarea, or when a modal overlay is active, preventing accidental shortcut collisions.
  - **Non-Color Visual Cues:** Visual states (e.g., center vs. outer tiles, valid vs. error states) must rely on shape, iconography, or explicit typography in addition to color contrast (WCAG 2.1 SC 1.4.1).
  - **Reduced Motion Support:** All shake and celebration animations must respect `@media (prefers-reduced-motion: reduce)` by disabling transitions or replacing them with static visual indicators.
- **NFR-03 (Performance):** Zero-lag user input response with optimized SVG rendering.
- **NFR-04 (UI Framework):** Visual layout is built primarily with **Bootstrap 5** utility/component classes and **Bootstrap Icons**, complemented by targeted custom SCSS for the honeycomb SVG, popovers, and bespoke animations — this corrects earlier drafts of this document, which stated Tailwind CSS.
- **NFR-05 (Testability / Separation of Concerns):** Business logic that doesn't strictly need to live inside `GameService` — scoring math (`ScoreService`) and career/streak statistics (`StatsService`) — is factored into dedicated singleton services for isolated unit testing, following the same rationale already documented for `StorageService` / `DictionaryService` / `PuzzleGeneratorService` in `service-component-contracts.md` §1.