# UI Structure & Layout Architecture — Beesagono

This document describes the user interface layout, visual hierarchy, Angular component structure, and responsive design guidelines for **Beesagono**.

> Layout is implemented primarily with **Bootstrap 5** utility/component classes and **Bootstrap Icons** (cards, navbar, modals, progress bars, badges, spinners), with bespoke SCSS reserved for the honeycomb SVG, popovers, and animations — this corrects earlier drafts of this document/`architecture.md`, which referenced Tailwind CSS.

---

## 1. Overall Layout Overview

```text
+-------------------------------------------------------+
|      HEADER / NAVBAR  (sticky, popovers: Rank,        |
|      Rules, Stats)          [Theme Toggle] [Share]    |
+-------------------------------------------------------+
|                                                       |
|                  WORD INPUT DISPLAY                   |
|              [  B E E S _  ]  <-- Typing              |
|              (Feedback Toasts / Shake)                |
|                                                       |
|                  HONEYCOMB GRID (SVG)                 |
|                       / \   / \                       |
|                      | A | | B |                      |
|                     / \ / \ / \ / \                   |
|                    | C | | E | | D |  <-- Center      |
|                     \ / \ / \ / \ /    (Gold)         |
|                      | F | | G |                      |
|                       \ /   \ /                       |
|                                                       |
|                   ACTION CONTROLS                     |
|           [ Delete ]  [ Shuffle ]  [ Enter ]          |
|                                                       |
+-------------------------------------------------------+
|                 DISCOVERED WORDS PANEL                |
|       Words Found (12): BEE, BEES, HONEY, ...         |
|                    [ list <-> word-map toggle ]       |
+-------------------------------------------------------+
|              WORDS BY LETTER (per-letter panel)       |
|   [A] 3/8 parole   [B] 1/2 parole   [C] 0/4 parole ...|
+-------------------------------------------------------+
|                 INVALID WORDS PANEL                   |
|       Parole Errate (3): XYZQ, ...                    |
+-------------------------------------------------------+
```
A one-time **Welcome / Disclaimer Modal** overlays the whole page on first launch, and an **End Game Modal** overlays it on puzzle completion.

---

## 2. Main View Components

### `HiveViewComponent` (root page, `/play` route)
Hosts the loading overlay, error+retry state, and — once ready — the full layout below. Owns the transient submit/share feedback toast state and global keyboard/visibility listeners.

### A. Header Component (`HeaderComponent`)
- **Logo & Title / Date:** Game title (*Beesagono*) + formatted date; click emits `logoClicked`.
- **Rank Badge → Scoreboard Popover:** Tapping the rank badge (desktop: centered; mobile: dedicated row below) opens `ScoreboardComponent` — a progress bar toward the next rank, with emoji markers for current position and next threshold.
- **Rules Button → Rules Popover (`RulesComponent`):** Game rules, scoring table, streak/season explainer, and the full rank-tier table.
- **Stats Button → Stats Popover (`StatsComponent`):** Career tier, KPI grid (games played, completion %, current/max streak), current season breakdown, local-storage disclaimer.
- **Share Button:** Emits `shareRequested`, handled by the parent's clipboard/Web-Share/`execCommand` fallback chain.
- **Theme Toggle:** Sun/moon icon button calling `ThemeService.toggleTheme()`.
- Popovers (Scoreboard/Rules/Stats) are mutually exclusive and all close on Escape.
- *(Storage Warning Banner: `GameService.isStorageAvailable` remains exposed for a non-blocking "progress won't persist" banner, but it is not currently rendered in the header template — noted here as a gap against the original design intent.)*

### B. Input Display & Toast Component (`WordDisplayComponent`)
- Purely presentational: receives `currentInput`, `centerLetter`, `feedbackMessage`, `feedbackType` as inputs from `HiveViewComponent`.
- **Feedback Toasts** mapped to `ValidationErrorType` (see `data-models.md` §6):

  | `errorType` | Toast copy (IT) |
  | :--- | :--- |
  | `TOO_SHORT` | "Parola troppo corta" |
  | `MISSING_CENTER` | "Manca la lettera centrale" |
  | `INVALID_LETTERS` | "Lettere non valide" |
  | `ALREADY_FOUND` | "Già trovata" |
  | `NOT_IN_DICTIONARY` | "Non è nella lista delle parole" |
- **Shake Animation:** Triggers CSS keyframe shake on validation error.

### C. Honeycomb Grid Component (`HoneycombGridComponent`)
- SVG canvas (`viewBox="0 0 320 320"`) rendering 7 hexagon paths; center at (160,160), outer tiles at radius 92, starting top-center and stepping 60°.
- Center Hexagon rendered with distinct golden styling (`#f59e0b`).
- Outer Hexagons (6×): neutral styling with hover/active click animations.
- Each tile is keyboard-focusable (`tabindex="0"`) and activates on Enter/Space in addition to click/tap; focus is removed after activation to avoid re-triggering.

### D. Action Controls Component (`HiveControlsComponent`, renamed from `ControlsComponent`)
- **Delete / Shuffle / Enter (Submit)** buttons; Submit has an internal ~200ms debounce guard against double activation.

### E. Found Words Panel Component (`FoundWordsComponent`, renamed from `FoundWordsPanelComponent`)
- Collapsible card (collapsed by default); word counter (`found/total`, plus `mielegrammi found/total` when applicable).
- Toggle between a flat word-badge list and a **word map** (`WordMapComponent`) dot-grid view.
- Mielegramma badges/cells highlighted distinctly.

### F. Words By Letter Component (`WordsByLetterComponent`) — NEW
- One expandable card per one of the 7 daily letters (center letter visually distinct), each with its own found/total word and Mielegramma counters and its own list/word-map toggle.
- All 7 cards start expanded; further user toggles persist across state changes.

### G. Word Map Component (`WordMapComponent`) — NEW
- Presentational dot-grid: one cell per possible word, colored by starting-letter palette once found (gold if Mielegramma), neutral/hidden ("Parola nascosta" tooltip) otherwise.

### H. Invalid Words Panel Component (`InvalidWordsComponent`) — NEW
- Collapsible card, **expanded by default**; counts and lists attempted words that failed validation (excluding duplicates of already-found words).

### I. Welcome / Disclaimer Modal (`WelcomeModalComponent`) — NEW
- Full-screen overlay shown once on first launch; explains no-login/local-only storage; single confirm button (`"Ho capito, fammi giocare!"`); Tab is trapped on that button, Escape dismisses; body scroll is locked while open.

### J. End Game Modal Component (`EndGameModalComponent`)
- Final score / words found / Mielegrammi found summary, with a "Condividi" (share) and a "Chiudi" (close) button.
- Full focus trap (Tab/Shift+Tab cycle within the dialog, prior focus restored on close); Escape closes.

---

## 3. Visual States & Color System

| State / Element | Color / Style | Purpose |
| :--- | :--- | :--- |
| **Center Hex Tile** | Gold / Amber (`#f59e0b`) | Mandatory letter |
| **Outer Hex Tile** | Light Gray / Slate (`#f1f5f9`) | Standard interactive tile |
| **Hover State** | Scale up | Desktop cursor feedback |
| **Active Click State** | Scale down | Tactile touch feedback |
| **Mielegramma Text / Cell** | Golden Glow | Pangram celebration |
| **Error Feedback / Invalid Word Badge** | Crimson Red (`#ef4444`) + Shake | Validation error |
| **Theme** | `data-theme="light"` / `"dark"` on `<html>` | Applied by `ThemeService`; follows OS preference until the player overrides it |

---

## 4. Loading, Error & Transition States

- **Initial Load Overlay:** Full-screen spinner with *"🐝 Preparazione dell'alveare..."*, shown while `dictionary.json` is fetched/parsed and the daily puzzle is generated (`GameService.loadStatus() === 'loading'`). Dismissed once both are complete.
- **Error State:** If loading fails, an alert box shows the error message (`GameService.loadError()`) with a **"Riprova"** retry button calling `retryLoadDailyGame()`.
- **Midnight Rollover Toast:** *"È iniziato un nuovo giorno! Caricamento del nuovo puzzle..."*, shown when the app detects a date change while open (see UC-06 in `use-cases.md`).
- **Submit / Share Feedback Toasts:** Transient success/error messages (owned by `HiveViewComponent`) auto-clear after 1.5–2s.

## 5. Responsive Design Guidelines

- **Mobile First:** Stacked layout optimized for touch interaction; the rank badge moves to its own centered row below the header's top row on mobile.
- **Desktop:** Centered fixed-width viewport (max-width `500px`).
- **Touch Targets:** Minimum size of **48x48px** for all tiles and buttons.