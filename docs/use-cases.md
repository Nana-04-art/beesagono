# Use Cases — Beesagono Word Game

## UC-01: Load Daily Game / Restore State
- **Actor:** Player
- **Precondition:** Application is opened in the browser.
- **Main Flow:**
  1. System checks current date string (`YYYY-MM-DD`), local to the browser.
  2. System deterministically generates today's daily puzzle using a PRNG seeded by the date string (1 center letter, 6 outer letters, total available words set, and daily Mielegrammi set), retrying with successive seeds until the puzzle satisfies the minimum quality gate (≥15 target words, ≥1 Mielegramma).
  3. While generation and dictionary loading are in progress, system displays a full-screen loading overlay ("Preparazione dell'alveare...").
  4. System checks `localStorage` for saved progress corresponding to today's date string, falling back to an in-memory store if `localStorage` is unavailable.
  5. If saved state for today exists, system restores score, found words list, and unlocked Mielegrammi.
  6. If saved state belongs to a previous date, system clears stale data and loads the new daily game.
  7. System renders the 7-hexagon honeycomb grid (Center letter in gold).

- **Alternate Flow (Storage Unavailable):**
  - If `localStorage` read/write fails (private browsing, quota exceeded), system proceeds as if no saved state exists, uses an in-memory store for the session, and shows a non-blocking banner warning that progress won't be saved.

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
  4. System provides a "Share Score" button that copies a Wordle-style plain-text summary to the clipboard, e.g.:
     ```
     Beesagono 23/07/2026
     Punteggio: 42/85 pts
     Parole trovate: 12
     Mielegrammi: 1/1
     ```

---

## UC-06: Daily Puzzle Rollover While App Is Open
- **Actor:** Player
- **Precondition:** Player has the app open (foreground or background) when local midnight passes.
- **Main Flow:**
  1. App regains foreground visibility (`visibilitychange` event fires).
  2. System compares current local date string against the date of the currently loaded puzzle.
  3. If they differ, system shows a toast: *"È iniziato un nuovo giorno! Caricamento del nuovo puzzle..."*
  4. System persists the previous day's final state as-is (no further writes).
  5. System generates and loads the new daily puzzle per UC-01, resetting score, found words, and Mielegrammi.