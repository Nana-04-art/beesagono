# Use Cases — Beesagono Word Game

## UC-01: Load Daily Game / Restore State — **(UPDATED)**
- **Actor:** Player
- **Precondition:** Application is opened in the browser (route `/play`).
- **Main Flow:**
  1. `HiveViewComponent.ngOnInit()` calls `WelcomeNoticeService.checkAndShowNotice()`; if this is the player's first launch (no `beesagono:mielegrammi_welcome_disclaimer_seen` flag), the Welcome/Disclaimer modal is shown first (see UC-10).
  2. If `GameService.loadStatus()` is `'idle'`, `loadDailyGame()` is invoked; otherwise `checkDateRollover()` runs instead (covers navigating back to an already-loaded session).
  3. `loadStatus` is set to `'loading'`; a full-screen overlay ("Preparazione dell'alveare...") is shown while `dictionary.json` is fetched, sanitized, and cached.
  4. The daily puzzle is generated deterministically via the **Candidate Pangrams Strategy** (a PRNG seeded by the local date string picks a whole 7-unique-letter dictionary word, derives the letter set and center letter from it), retrying with successive RNG draws until the puzzle satisfies the Quality Gate (≥15 target words, ≥1 Mielegramma), capped at 500 attempts with a guaranteed fallback board.
  5. System checks `localStorage` for a valid, version-matching saved `GameState` for today, falling back to an in-memory store if `localStorage` is unavailable.
  6. If valid saved state exists, score, found words, invalid words, and unlocked Mielegrammi are restored; otherwise a fresh `GameState` is initialized.
  7. `StatsService.recordGameStarted(today)` updates the play-streak and games-played counters.
  8. `loadStatus` becomes `'ready'`; the 7-hexagon honeycomb grid renders (center letter in gold), along with the header, found/invalid words panels, and words-by-letter breakdown.

- **Alternate Flow (Storage Unavailable):**
  - If `localStorage` read/write fails, the app proceeds as if no saved state exists, uses an in-memory store for the session, and `GameService.isStorageAvailable()` reflects the degraded state.

- **Alternate Flow (Load Failure):**
  - If the dictionary fetch/parse fails, `loadStatus` becomes `'error'` with a descriptive `loadError()` message; the UI shows an alert with a **"Riprova"** button that calls `retryLoadDailyGame()`, re-attempting the whole flow.

---

## UC-02: Compose Word (Keyboard or Mouse) — unchanged
- **Actor:** Player
- **Precondition:** Daily game is loaded and active (`loadStatus === 'ready'`).
- **Main Flow:**
  - **Option A (Keyboard):** Player types physical letter keys.
  - **Option B (Mouse/Touch):** Player clicks/taps any of the 7 interactive SVG hexagons (or activates one via Enter/Space while focused).
  1. System appends the selected letter to `currentInput`.
  2. System highlights the corresponding hexagon on screen during press/click.
  3. Player can use `Backspace` or the on-screen "Delete" button to remove the last character.

---

## UC-03: Shuffle Outer Letters
- **Actor:** Player
- **Precondition:** Game is active.
- **Main Flow:**
  1. Player clicks "Shuffle" (`HiveControlsComponent`, debounced against rapid double-activation).
  2. System randomly reorders the 6 outer letters on the SVG grid.
  3. The center mandatory letter remains locked in position.

---

## UC-04: Submit & Validate Word — **(UPDATED)**
- **Actor:** Player
- **Precondition:** Player presses `Enter` or clicks "Submit."
- **Main Flow:**
  1. System validates `currentInput` in order: Length ≥ 4 → Contains center letter → Uses only the 7 daily letters → Not already found → Present in `targetWords`.
  2. **If Rules 1, 2, 3, or 5 fail:**
     - The attempted word is appended (de-duplicated) to `invalidWords` and persisted alongside the day's `GameState`, then surfaced in the Invalid Words panel.
     - System displays a specific error toast and triggers a shake animation on the input field.
  3. **If Rule 4 fails (duplicate):** an "Already Found" toast and shake are shown, but the word is **not** added to `invalidWords`.
  4. **If ALL Rules Pass:**
     - System calculates awarded points (4-letter = 1pt; >4-letter = 1pt/letter) and checks for a **Mielegramma** (+7 bonus, gold celebration visuals).
     - The word is added to `foundWords`/`foundMielegrammi`; score, rank, and persistence update; `StatsService.recordProgress(date, newScore, isCompleted, rankLabel)` is called to keep season/streak stats current.
     - `currentInput` is cleared.
     - If all target words are found, **UC-05 (Complete Daily Puzzle)** triggers ~600ms later.

---

## UC-05: Complete Daily Puzzle / View Summary — **(UPDATED share flow)**
- **Actor:** Player
- **Precondition:** Player finds all valid words for today's puzzle.
- **Main Flow:**
  1. System sets `isCompleted` to `true` and, after a short delay, opens the End Game Modal.
  2. The modal presents final statistics: score/max score, words found/total, Mielegrammi found/total.
  3. The "Condividi" (Share) button attempts, in order: (a) `navigator.clipboard.writeText()`, (b) the Web Share API (`navigator.share`, primarily mobile), (c) a legacy `document.execCommand('copy')` fallback — surfacing a transient success/error toast for whichever step succeeds. Payload/format unchanged (Wordle-style, `DD/MM/YYYY`).
  4. The modal traps keyboard focus (Tab/Shift+Tab cycle, prior focus restored on close) and closes on Escape or the "Chiudi" button.

---

## UC-06: Daily Puzzle Rollover While App Is Open — **(UPDATED detection trigger)**
- **Actor:** Player
- **Precondition:** Player has the app open (foreground or background) when local midnight passes.
- **Main Flow:**
  1. Either the `visibilitychange` event fires with the tab visible, or the window regains `focus` while the tab is visible (`HiveViewComponent` listens to both).
  2. System compares the current local date against the loaded `GameBoard.date`.
  3. If they differ, a toast is shown: *"È iniziato un nuovo giorno! Caricamento del nuovo puzzle..."*
  4. The previous day's final state is persisted as-is (no further writes).
  5. System generates and loads the new daily puzzle per UC-01, resetting score, found/invalid words, and Mielegrammi, and calls `StatsService.recordGameStarted` for the new day.

---

## UC-07: Track Daily Streak & Season Statistics — **(NEW)**
- **Actor:** Player
- **Precondition:** Player starts or progresses in a game on any day.
- **Main Flow:**
  1. On the first play of a new local calendar day, `StatsService` increments `gamesPlayed` and the current streak (or resets it to 1 if a day was skipped), updating `maxStreak` if a new record was set.
  2. As the day's score increases, the season's `basePointsEarned` grows by the score delta; on reaching a streak-length milestone (3/7/15/30/50/100/200/365 days) for the first time this season, a one-time bonus is added to `bonusStreakPoints`.
  3. On calendar-year rollover, the current season is archived into `seasonHistory` and a fresh season begins.
  4. On puzzle completion, `gamesCompleted` increments (once per day); the day's achieved rank label increments `dailyRankDistribution[rankLabel]`.
  5. The career tier (§9b of `data-models.md`) is recalculated from the season's total points relative to an estimated day-of-year maximum.
  6. If no aggregate `beesagono:stats` blob is found (e.g. right after this feature ships), `StatsService` rebuilds the full history by replaying every stored `beesagono:game:*` entry chronologically.

---

## UC-08: Toggle Light/Dark Theme — **(NEW)**
- **Actor:** Player
- **Precondition:** App is loaded (any `loadStatus`).
- **Main Flow:**
  1. On first load, `ThemeService` reads `beesagono:user_theme`; if unset, it falls back to the OS `prefers-color-scheme` setting.
  2. Player taps the sun/moon toggle in the header.
  3. `currentTheme` flips between `'light'`/`'dark'`; the `data-theme` attribute on `<html>` updates immediately, and the choice is persisted.

---

## UC-09: View Rules, Rank Progress, or Career Stats — **(NEW)**
- **Actor:** Player
- **Precondition:** App is `'ready'`.
- **Main Flow:**
  1. Player taps the Rules button, the rank badge, or the Stats button in the header.
  2. The corresponding popover (Rules / Scoreboard / Stats) opens; opening one automatically closes any other open popover (mutually exclusive).
  3. Player dismisses the popover by tapping its trigger again, tapping elsewhere, or pressing Escape (closes all).

---

## UC-10: First-Launch Welcome & Storage Disclaimer — **(NEW)**
- **Actor:** Player (first-time visitor to this browser/device)
- **Precondition:** No `beesagono:mielegrammi_welcome_disclaimer_seen` flag is stored (or storage access throws an error, e.g. `SecurityError`).
- **Main Flow:**
  1. On app init, the Welcome/Disclaimer modal opens automatically, explaining that no account is required and that progress is saved locally to this browser/device only.
  2. Body scroll is locked; keyboard focus is trapped on the single "Ho capito, fammi giocare!" confirm button (Tab always returns focus to it).
  3. Player confirms (or presses Escape); the modal closes, body scroll is restored, and the "seen" flag is persisted (write failures are silently ignored so the modal never re-blocks play due to a storage error).

---

## UC-11: Browse Words by Letter / Word Map View — **(NEW)**
- **Actor:** Player
- **Precondition:** Game is `'ready'`.
- **Main Flow:**
  1. In the Found Words panel or the Words-by-Letter panel, the player toggles between the flat word-badge list and the **word map** dot-grid view.
  2. In the word map, each possible word is a cell: colored by its starting letter once found (gold if a Mielegramma), neutral and unlabeled ("Parola nascosta") if not yet found.
  3. In Words-by-Letter, the player can independently expand/collapse and toggle the map view for each of the 7 daily letters; all 7 groups are expanded by default on first load, and any manual toggles the player makes are preserved as the game state changes.