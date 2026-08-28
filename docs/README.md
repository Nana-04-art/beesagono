# Beesagono

**Beesagono** is a reactive Progressive Web Application (PWA) inspired by the famous *Spelling Bee* game by The New York Times (Italian version based on letter combinations and pangrams — "Mielegrammi"). It is built with **Angular 21**, utilizing **Angular Signals** for reactive state management and **Scalable Vector Graphics (SVG)** for rendering the interactive game board.

Beyond the core daily word puzzle, Beesagono tracks a persistent **career/season progression system** (daily streaks, seasonal points, career tiers) independent from the daily rank, and supports **light/dark theming** — all stored locally in the browser, with no backend or account required.

---

## Project Documentation

The complete software analysis and technical architecture are fully documented within the [`/docs`](./docs) directory:

**Frontend & Core Domain**
* [Functional & Non-Functional Requirements](requirements.md)
* [Use Cases](use-cases.md)
* [Logic Flowchart](flowchart.md)
* [Frontend System Architecture & Signals](frontend-architecture.md)
* [UI Structure & SVG Layout](ui-structure.md)
* [Frontend Data Models](frontend-data-models.md)
* [Service & Component Public Contracts](service-component-contracts.md)

**Backend Architecture & API**
* [Backend Architecture & Security](backend-architecture.md)
* [REST API Contracts](api-contracts.md)
* [Backend Data Model & Schema](backend-data-models.md)
* [Guest ↔ Account Sync Strategy](account-sync-strategy.md)

---

## Tech Stack

**Frontend**
* **Framework:** Angular 21 (Standalone Components)
* **State Management:** Angular Signals (`signal`, `computed`, `effect`)
* **Graphics & UI:** Interactive SVG honeycomb grid layered with bespoke SCSS
* **UI Framework:** Bootstrap 5 (utility classes + components — cards, modals, navbar, progress bars, badges, spinners) and Bootstrap Icons, layered with bespoke SCSS for the hex grid, popovers, and animations
* **Client Persistence:** Browser LocalStorage (`beesagono:`-prefixed keys) with automatic in-memory fallback
* **Language:** TypeScript 5.x
* **Testing:** Vitest + Angular `TestBed`

**Backend**
* **Runtime & Framework:** Java 21, Spring Boot 4.0.3 (Spring Web, Spring Data JPA, Spring Security)
* **Database Target:** MySQL & Oracle (ANSI-standard portable schema, UUID primary keys)
* **Security:** Stateless JWT (Short-lived access tokens + server-revocable refresh tokens), BCrypt hashing
* **API Protocol:** REST / JSON under `/api/v1`

---

## Key Features at a Glance

* **Deterministic Daily Puzzle:** Server-side lazy puzzle generation (7-hexagon honeycomb, 1 mandatory center letter) matching deterministic PRNG seeds across all platforms.
* **Tamper-Proof Validation:** Zero-lag word validation backed by server-side dictionary checks, anti-cheat score computing, and rank tier resolution.
* **Guest Onboarding & Hybrid Storage:** Play immediately as a guest with browser `localStorage` persistence[cite: 7, 11]. On registration or login, local progress is validated and merged into the account.
* **Multi-Device & Offline Sync:** Resume games on any device, merge offline/guest sessions seamlessly, and maintain your streak across platforms.
* **Detailed Gameplay Analytics:** "Words by letter" breakdown, invalid-attempt tracking, and a pangram-style "word map" dot-grid visualization.
* **Seasonal Career Progression:** Daily streaks, milestone bonus points, rank histograms, and yearly career tiers — separate from the daily rank.
* **Theming & UX:** Light/dark theme toggle (defaulting to OS preference) and a first-launch welcome modal explaining guest storage.
* **Score Sharing:** Share score and grid emojis via the Web Share API, Clipboard API, or legacy `execCommand` fallbacks.