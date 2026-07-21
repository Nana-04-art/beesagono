## 1. Functional Requirements (FR)

### Grid & Visual Representation
- **FR-01 (7-Hexagon Honeycomb Layout):** The board must render a 7-hexagon honeycomb grid: 1 central mandatory hexagon and 6 surrounding outer hexagons.
- **FR-02 (Hexagon Visual States):**
  - **Center Hexagon:** Highlighted with a distinct golden/amber theme to denote mandatory usage.
  - **Outer Hexagon:** Standard tile styling with active/hover animation states.
  - **Mielegram (Pangram):** Gold animation effects triggered when a word using all 7 letters is discovered or played.

### User Input Management
- **FR-03 (Keyboard Input):** Support physical keyboard inputs (letter keys, Backspace, Enter).
- **FR-04 (Mouse/Touch Input):** Support clicking on SVG/Canvas hexagons, alongside action controls (Delete, Shuffle, Submit).
- **FR-05 (Shuffle Mechanism):** Shuffling must re-organize the 6 outer letters visually while keeping the center letter fixed.

### Word Validation Rules
- **FR-06 (Validation Constraints):** Upon submission, check rules in order:
  1. Minimum length (at least 4 letters).
  2. Must contain the center mandatory letter.
  3. Must only use letters from the daily set of 7.
  4. Must not be a previously submitted word.
  5. Must exist in the official dictionary.
- **FR-07 (Error Feedback):** Display specific toast/toastlet errors (e.g., "Missing center letter", "Too short", "Not in dictionary") with a visual shake animation.

### Scoring & Game Progress
- **FR-08 (Scoring Engine):**
  - 4-letter words = 1 point.
  - Words > 4 letters = 1 point per letter.
  - **Mielegram Bonus:** Extra bonus points awarded when all 7 distinct letters are used.
- **FR-09 (Discovered Words List):** Display a accordion/scrollable list of correctly found words.
- **FR-10 (Local Persistence):** Automatically store current score and found words list in `localStorage`.

---

## 2. Non-Functional Requirements (NFR)
- **NFR-01 (Architecture):** Angular 21 with Standalone Components and Signals for state management (`wordInput`, `foundWords`, `score`).
- **NFR-02 (Accessibility):** Full keyboard accessibility for non-mouse navigation.
- **NFR-03 (Performance):** Zero-lag user input response with optimized SVG rendering.