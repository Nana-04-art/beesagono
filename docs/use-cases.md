# Use Cases — Beesagono Word Game

## UC-01: Load Daily Game / Restore State
- **Actor:** Player
- **Precondition:** Application is opened in the browser.
- **Main Flow:**
  1. System loads the daily puzzle (1 center letter, 6 outer letters, valid dictionary list).
  2. System checks `localStorage` for existing progress for today's puzzle.
  3. If saved state exists, system restores score, found words list, and unlocked Mielegrammi.
  4. System renders the 7-hexagon honeycomb grid (Center letter in gold).

---

## UC-02: Compose Word (Keyboard or Mouse)
- **Actor:** Player
- **Precondition:** Game is loaded and active.
- **Main Flow:**
  - **Option A (Keyboard):** Player types physical letter keys on keyboard.
  - **Option B (Mouse/Touch):** Player clicks on any of the 7 interactive SVG hexagons.
  1. System appends the selected letter to the active input stream (`currentInput` signal).
  2. System highlights the corresponding hexagon on screen during press/click.
  3. Player can use `Backspace` key or on-screen "Delete" button to remove the last character.

---

## UC-03: Shuffle Outer Letters
- **Actor:** Player
- **Precondition:** Game is active.
- **Main Flow:**
  1. Player clicks the "Shuffle" button.
  2. System randomly reorders the position of the 6 outer letters on the SVG grid.
  3. The center mandatory letter remains locked in its central position.

---

## UC-04: Submit & Validate Word
- **Actor:** Player
- **Precondition:** Player presses `Enter` key or clicks "Submit" button.
- **Main Flow:**
  1. System validates the `currentInput` string against the game constraints in order:
     - **Rule 1 (Length):** Is word length ≥ 4 letters?
     - **Rule 2 (Center Letter):** Does word contain the mandatory center letter?
     - **Rule 3 (Valid Character Set):** Are all letters belonging to the 7 daily letters?
     - **Rule 4 (Duplicate Check):** Is this word already in the `foundWords` array?
     - **Rule 5 (Dictionary Check):** Does the word exist in the official dictionary?
  2. **If ANY Rule Fails:**
     - System displays a specific error toast (e.g., *"Missing center letter"*, *"Not in dictionary"*).
     - System triggers a visual shake animation on the text input field.
  3. **If ALL Rules Pass:**
     - System calculates awarded points (4 letters = 1 pt, >4 letters = 1 pt per letter).
     - System checks if the word is a **Mielegramma** (uses all 7 letters).
     - If Mielegramma, system awards bonus points and triggers gold celebration visuals.
     - System adds word to `foundWords` list, updates total score signal, and saves state to `localStorage`.
     - System clears `currentInput`.