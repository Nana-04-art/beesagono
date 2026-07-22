# Use Cases — Beesagono Word Game

## UC-01: Load Daily Game / Restore State
- **Actor:** Player
- **Precondition:** Application is opened in the browser.
- **Main Flow:**
  1. System checks current date string (`YYYY-MM-DD`).
  2. System generates today's daily puzzle (1 center letter, 6 outer letters, total available words set, and daily Mielegrammi set).
  3. System checks `localStorage` for saved progress corresponding to today's date string.
  4. If saved state for today exists, system restores score, found words list, and unlocked Mielegrammi.
  5. If saved state belongs to a previous date, system clears stale data and loads the new daily game.
  6. System renders the 7-hexagon honeycomb grid (Center letter in gold).

---

## UC-02: Compose Word (Keyboard or Mouse)
- **Actor:** Player
- **Precondition:** Daily game is loaded and active.
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
  2. System randomly reorders the visual position of the 6 outer letters on the SVG grid.
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
     - **Rule 4 (Duplicate Check):** Is this word already in the `foundWords` set?
     - **Rule 5 (Target Words Check):** Is the word present in today's pre-calculated playable words set?
  2. **If ANY Rule Fails:**
     - System displays a specific error toast (e.g., *"Missing center letter"*, *"Not in word list"*).
     - System triggers a visual shake animation on the text input field.
  3. **If ALL Rules Pass:**
     - System calculates awarded points (4-letter words = 1 pt, > 4 letters = 1 pt per letter).
     - System checks if the word is a **Mielegramma** (word containing all 7 unique daily letters).
     - If Mielegramma, system awards +7 bonus points and triggers gold celebration visuals.
     - System adds word to `foundWords` set, updates score, and syncs state to `localStorage`.
     - System clears `currentInput`.
     - If all daily target words are discovered, system triggers **UC-05 (Complete Daily Puzzle)**.

---

## UC-05: Complete Daily Puzzle / View Summary
- **Actor:** Player
- **Precondition:** Player finds all valid words for today's puzzle.
- **Main Flow:**
  1. System sets `isCompleted` state flag to `true`.
  2. System displays the **Game Completed Modal / End Game Screen**.
  3. System presents final summary statistics: total score achieved out of max score, percentage of words found, list of Mielegrammi found vs total Mielegrammi available, and time elapsed.
  4. System provides a "Share Score" button to copy daily results to clipboard.