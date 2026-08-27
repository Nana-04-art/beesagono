# System Architecture & Technical Specifications — Beesagono

This document defines the high-level technical architecture, framework choices, core service logic, and persistence strategy for **Beesagono**. See `service-component-contracts.md` for exact method/signal signatures.

---

## 1. Architectural Overview

**Beesagono** is a lightweight, single-page progressive web application (PWA) built with **Angular 21**, using a **Client-Side Only Architecture**: all game logic, daily puzzle generation, and state management run inside the browser. Daily puzzle generation is **deterministic** (Candidate Pangrams Strategy — see `data-models.md` §8): a PRNG seeded from the local date string ensures every player gets the identical puzzle on the same calendar day with no server involved. Beyond a single day's puzzle, the app also maintains a **season/career statistics layer** (streaks, seasonal points, career tiers) that persists and evolves independently across days.

```text
+-----------------------------------------------------------------------------+
|                          BROWSER / CLIENT                                   |
|                                                                             |
|   +---------------------------------------------------------------------+   |
|   |                      PRESENTATION LAYER (routed at /play)           |   |
|   | HiveViewComponent: Header (+ Scoreboard/Rules/Stats popovers),      |   |
|   | Word Display, SVG Honeycomb, Controls, Found/Words-by-Letter/       |   |
|   | Invalid Words panels, Welcome Modal, End Game Modal                 |   |
|   +----------------------------------+----------------------------------+   |
|                                      |                                      |
|                                      v                                      |
|   +---------------------------------------------------------------------+   |
|   |                          SERVICE LAYER                              |   |
|   |  GameService (loads dictionary.json, generates the daily puzzle,    |   |
|   |  extracts targetWords & Mielegrammi Sets, orchestrates submit flow) |   |
|   |  ScoreService (scoring math, rank lookups)                          |   |
|   |  StatsService (streaks, seasons, career tiers)                      |   |
|   |  ThemeService  |  WelcomeNoticeService                              |   |
|   +------------------+----------------------------+---------------------+   |
|                      |                            |                         |
|          +-----------+-----------+   +------------+------------+            |
|          |                       |   |                         |            |
|          v                       v   v                         v            |
|   +---------------+    +--------------------+      +--------------------+   |
|   | IN-MEMORY     |    | PERSISTENCE LAYER  |      | DictionaryService /|   |
|   | TARGET SETS   |    | StorageService:    |      | PuzzleGenerator-   |   |
|   | (O(1) word    |    | localStorage,      |      | Service (pure,     |   |
|   | validation)   |    | beesagono:*-       |      | deterministic)     |   |
|   |               |    | prefixed keys,     |      |                    |   |
|   |               |    | in-memory fallback |      |                    |   |
|   +---------------+    +--------------------+      +--------------------+   |
+-----------------------------------------------------------------------------+
```
---

## 2. Technology Stack & Key Decisions

| Tech / Library | Selection | Rationale |
| :--- | :--- | :--- |
| **Framework** | Angular 21 | High-performance reactive framework using modern Standalone Components and Signals. |
| **State Management** | Angular Signals | Built-in reactive primitives (`signal`, `computed`, `effect`) eliminating external state libraries. |
| **Rendering Engine** | Scalable Vector Graphics (SVG) | Native vector math for scalable, crisp hexagonal tiles across all screen resolutions. |
| **Persistence** | Browser `localStorage` | Preserves game progress, aggregate stats, and preferences across reloads without a backend, all under a `beesagono:` key namespace. |
| **Styling** | **Bootstrap 5** (utility classes + components) + **Bootstrap Icons**, plus custom SCSS | Fast, consistent layout/components (cards, modals, navbar, progress bars, badges) with bespoke SCSS reserved for the hex grid, popovers, and animations. *(Corrects earlier drafts of this document, which stated Tailwind CSS.)* |
| **Routing** | Angular Router, single lazy route (`/play`) | The gameplay page (`HiveViewComponent`) is loaded via `loadComponent`, decoupling the route shell from the page implementation. |

---

## 3. Core Component Architecture

* **`HiveViewComponent`** (`pages/hive-view/`, root of the `/play` route): layout shell, global keyboard/visibility HostListeners, transient submit/share feedback state.
* **`HeaderComponent`**: game logo/date, rank badge, and Rules/Scoreboard/Stats popovers, share button, theme toggle.
* **`ScoreboardComponent` / `RulesComponent` / `StatsComponent`**: header popovers (rank progress, static rules, career/season stats respectively).
* **`HoneycombGridComponent`**: SVG-based component drawing the 7 hexagonal tiles and handling click/touch/keyboard input.
* **`WordDisplayComponent`**: presentational live-typed string, feedback toasts, shake animation.
* **`HiveControlsComponent`**: Delete / Shuffle / Enter buttons (renamed from `ControlsComponent`, debounced submit).
* **`FoundWordsComponent`**: found-words list / word-map toggle (renamed from `FoundWordsPanelComponent`).
* **`WordsByLetterComponent` / `WordMapComponent`**: per-letter breakdown and pangram-style dot-grid visualization.
* **`InvalidWordsComponent`**: tracked invalid submission attempts.
* **`WelcomeModalComponent`**: first-launch disclaimer.
* **`EndGameModalComponent`**: completion summary + share.

---

## 4. State Management & Service Layer

### `GameService`
Centralized reactive state for the active day's game (`board`, `loadStatus`, `currentInput`, `foundWords`, `invalidWords`, `score`, `rank`, `displayCells`, `wordMap`, `letterColors`, ...). Key responsibilities:
1. **Load Orchestration:** fetches the dictionary, generates/restores the daily board, and exposes `'idle' | 'loading' | 'ready' | 'error'` load status with a retry path.
2. **Input Handling:** appends/deletes characters from `currentInput` via keyboard, tile clicks/taps, or the reserved `'BACKSPACE'`/`'ENTER'` tokens.
3. **Shuffle Logic:** randomly shuffles the 6 outer `Cell` positions while keeping index 0 (center) immutable.
4. **Validation Pipeline:** evaluates submissions against the FR-06 rules, tracking non-duplicate failures into `invalidWords`.
5. **Auto-Save Sync:** an Angular `effect()` mirrors state (including `invalidWords` and a `rankLabel` snapshot) into `localStorage` via `StorageService` whenever `loadStatus() === 'ready'`.
6. **Stats Hand-off:** notifies `StatsService.recordGameStarted` / `recordProgress` on load and on each successful submission.

### `ScoreService` (extracted from `GameService`)
Pure scoring math (`calculateWordPoints`, `calculateTotalScore`) and rank lookups (`getCurrentRank`, `getNextRank`, `getPointsToNext`, `calculatePercentage`) — isolated for unit testing and reused by `GameService`, `ScoreboardComponent`, and elsewhere.

### `StatsService`
Owns the **career/season/streak** layer, independent of and outliving any single `GameState`:
- **Streaks:** increments once per first play of a new local calendar day; resets to 1 on a missed day; tracks the historical max.
- **Seasons:** one per calendar year; accumulates base points (cumulative daily scores) + one-time streak-milestone bonus points (3/7/15/30/50/100/200/365-day thresholds).
- **Career Tier:** derived from season total vs. an estimated day-of-year max, mapped against `CAREER_TIERS`.
- **Recovery:** if no `beesagono:stats` blob exists, rebuilds itself by scanning and replaying every `beesagono:game:*` entry chronologically via `StorageService.getKeysByPrefix` + `game-state.validator.ts`'s `isValidGameState`.

### `ThemeService`
Light/dark theme signal, backed by `beesagono:user_theme` with an OS-preference fallback (`prefers-color-scheme`), applied via a `data-theme` attribute on `<html>`.

### `WelcomeNoticeService`
Tracks whether the first-launch disclaimer has been dismissed (`beesagono:mielegrammi_welcome_disclaimer_seen`), failing open (shows the notice) on any storage read error.

### `StorageService`
A dedicated service wrapping all `localStorage` reads/writes in `try/catch`, transparently namespacing every key with `beesagono:`. On failure (blocked storage, quota exceeded, or an invalid key/payload), it falls back to an in-memory store for the session and (for `GameService`'s use) surfaces a non-blocking warning. Also exposes `getKeysByPrefix()`, merging real `localStorage` and the in-memory fallback, used by `StatsService` to rebuild history.

---

## 5. Performance Strategy

* **Dictionary Lookup (O(1) Complexity):** the daily dictionary list is stored in a JavaScript `Set`, guaranteeing constant-time lookup during word submission.
* **Event Delegation:** global keyboard input is bound via standard Angular HostListeners on `window`/`document` in `HiveViewComponent`, capturing `Backspace`, `Enter`, and character input, while ignoring input when a modal/popover with its own focus context is open.
* **Deterministic, Client-Only Puzzle Generation:** the Candidate Pangrams Strategy (§8 of `data-models.md`) runs entirely client-side against the pre-loaded dictionary `Set`, capped at 500 attempts, with a guaranteed non-empty fallback board.