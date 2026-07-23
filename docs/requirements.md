# Functional & Non-Functional Requirements — Beesagono

> See `data-models.md` for TypeScript interfaces, the full puzzle generation algorithm, rank thresholds, and storage schema referenced throughout this document.

## 1. Functional Requirements (FR)

### Grid & Visual Representation
- **FR-01 (7-Hexagon Honeycomb Layout):** The board must render a 7-hexagon honeycomb grid: 1 central mandatory hexagon and 6 surrounding outer hexagons.
- **FR-02 (Hexagon Visual States):**
  - **Center Hexagon:** Highlighted with a distinct golden/amber theme to denote mandatory usage.
  - **Outer Hexagon:** Standard tile styling with active/hover animation states.
  - **Mielegramma (Pangram):** Gold celebration effects triggered when a word using all 7 daily letters is discovered.

### User Input Management
- **FR-03 (Unified Input Handling):** The application must process all letter entries—whether coming from physical keyboard presses, mouse clicks, or touch screen taps on SVG hexagons—through a single, unified input handler method (`handleInput`).
  - Every character is normalized to uppercase (`.toUpperCase()`).
  - Non-alphabetic characters (numbers, symbols, whitespace) are silently discarded, never appended to `currentInput`.
  - On mobile/touch devices, the native virtual keyboard is disabled (`readonly` / `inputmode="none"` on the input field) to avoid layout overlay issues; input is exclusively via the on-screen hexagons and controls.
- **FR-04 (Action Controls):** Provide dedicated control handlers for non-character interactions (*Delete*, *Shuffle*, *Submit*).
- **FR-05 (Shuffle Mechanism):** Shuffling must re-organize the 6 outer letters visually on the grid while keeping the mandatory center letter fixed.

### Daily Game Specifications
- **FR-ALG-01 (Midnight Puzzle Renewal):** At midnight (00:00, **local browser timezone** — not UTC) or upon the first application launch of the day, the system automatically clears the previous session and generates a brand-new daily puzzle (1 center letter and 6 outer letters). While the app remains open across midnight, detection happens via the `visibilitychange` event: when the tab/PWA regains foreground focus, the system compares the current local date against the loaded puzzle's date and triggers renewal on mismatch, showing a toast to the player before swapping puzzles.
- **FR-ALG-02 (Pre-Calculated Daily Sets):** Upon puzzle generation, `GameService` extracts:
  1. `targetWords`: All valid dictionary words containing the center letter and using only the 7 daily letters (length ≥ 4).
  2. `mielegrammi`: Words in `targetWords` that use all 7 unique daily letters.
- **FR-ALG-03 (Zero-Lag In-Memory Validation):** `GameService` holds `targetWords` in an in-memory `Set` for instant $O(1)$ validation without querying external services during gameplay.
- **FR-ALG-04 (Deterministic Generation):** The daily puzzle is generated deterministically from a PRNG seeded by the local date string (`YYYY-MM-DD`), so every player sees the identical puzzle on the same calendar day, with no server round-trip required.
- **FR-ALG-05 (Puzzle Quality Gate):** A candidate 7-letter set is only accepted if it yields at least 15 target words and at least 1 Mielegramma. If the gate fails, the generator retries with the next PRNG seed (capped at 500 attempts) until a valid board is found.

### Word Validation Rules
- **FR-06 (Validation Constraints):** Upon submission, check constraints in sequence:
  1. Minimum length (at least 4 letters).
  2. Contains the mandatory center letter.
  3. Uses only allowed letters from the daily set of 7.
  4. Is not already in the `foundWords` set.
  5. Exists in today's pre-calculated `targetWords` set.
- **FR-07 (Error Feedback):** Display specific error toasts (e.g., *"Missing center letter"*, *"Too short"*, *"Not in word list"*) alongside a visual shake animation on the input field.

### Scoring & Game Progress
- **FR-08 (Scoring Engine):**
  - 4-letter words = 1 point.
  - Words > 4 letters = 1 point per letter.
  - **Mielegramma Bonus:** +7 bonus points awarded when all 7 distinct letters are used.
- **FR-09 (Discovered Words List):** Display a scrollable list of correctly found words and highlight discovered Mielegrammi.
- **FR-10 (Local Persistence):** Automatically persist current date, score, and found words set in `localStorage` keyed by today's date string (`beesagono:game:{YYYY-MM-DD}`), including a `version` field for future schema migrations.
- **FR-10a (Persistence Failure Fallback):** If `localStorage` is unavailable or blocked (e.g. Safari private browsing, quota exceeded), the app must fall back transparently to an in-memory store for the current session and display a non-blocking warning that progress will not survive a page reload.
- **FR-11 (End Game Screen):** Display a completion summary modal when all words are found or the game ends, showing final statistics and a share score feature.
- **FR-11a (Share Score Format):** The share feature copies a Wordle-style plain-text summary to the clipboard, including the date (`DD/MM/YYYY`), score/max score, words found, and Mielegrammi found/total.
- **FR-12 (Rank Progression):** The player's rank is derived (never persisted) from `score / maxScore` percentage and displayed reactively in the header. See rank threshold table in `data-models.md`, Section 5.

---

## 2. Non-Functional Requirements (NFR)
- **NFR-01 (Architecture):** Angular 21 with Standalone Components and Signals for state management (`wordInput`, `foundWords`, `score`).
- **NFR-02 (Accessibility):** Full keyboard accessibility for non-mouse navigation.
- **NFR-03 (Performance):** Zero-lag user input response with optimized SVG rendering.