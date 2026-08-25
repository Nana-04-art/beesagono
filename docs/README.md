# Beesagono

**Beesagono** is a reactive Progressive Web Application (PWA) inspired by the famous *Spelling Bee* game by The New York Times (Italian version based on letter combinations and pangrams — "Mielegrammi"). It is built with **Angular 21**, utilizing **Angular Signals** for reactive state management and **Scalable Vector Graphics (SVG)** for rendering the interactive game board.

Beyond the core daily word puzzle, Beesagono tracks a persistent **career/season progression system** (daily streaks, seasonal points, career tiers) independent from the daily rank, and supports **light/dark theming** — all stored locally in the browser, with no backend or account required.

---

## Project Documentation

The complete software analysis and technical architecture are fully documented within the [`/docs`](./docs) directory:

* [Functional & Non-Functional Requirements](requirements.md)
* [Use Cases](use-cases.md)
* [Logic Flowchart](flowchart.md)
* [System Architecture & Signals](architecture.md)
* [UI Structure & SVG Layout](ui-structure.md)
* [Data Models](data-models.md)
* [Service & Component Public Contracts](service-component-contracts.md)

---

## Tech Stack

* **Frontend Framework:** Angular 21 (Standalone Components)
* **State Management:** Angular Signals (`signal`, `computed`, `effect`)
* **Graphics & UI:** Interactive SVG (Scalable Vector Graphics) for the honeycomb board
* **UI Framework:** Bootstrap 5 (utility classes + components — cards, modals, navbar, progress bars, badges, spinners) and Bootstrap Icons, layered with bespoke SCSS for the hex grid, popovers, and animations
* **Persistence:** Browser LocalStorage (`beesagono:`-prefixed keys), with automatic in-memory fallback when storage is blocked/full
* **Language:** TypeScript 5.x
* **Testing:** Vitest + Angular `TestBed`

---

## Key Features at a Glance

- Deterministic daily puzzle (7-hexagon honeycomb, 1 mandatory center letter), generated client-side with no server round-trip
- Zero-lag O(1) word validation against a pre-computed daily target-word set
- Local persistence of daily progress, with schema versioning and a transparent in-memory fallback
- Invalid-attempt tracking alongside found words
- "Words by letter" breakdown and a pangram-style "word map" dot-grid visualization
- Seasonal career progression: daily streaks, milestone bonus points, and career tiers — separate from the daily rank
- Light/dark theme toggle, defaulting to the OS preference
- First-launch welcome/disclaimer modal explaining local-only storage
- Share score via the Clipboard API, with Web Share API and legacy `execCommand` fallbacks