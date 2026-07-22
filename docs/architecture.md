# System Architecture & Technical Specifications — Beesagono

This document defines the high-level technical architecture, framework choices, core service logic, and persistence strategy for **Beesagono**.

---

## 1. Architectural Overview

**Beesagono** is designed as a lightweight, single-page progressive web application (PWA) built with **Angular 21**. It follows a **Client-Side Only Architecture** where all game logic, daily puzzle generation, and state management run directly within `GameService` inside the user's browser.

```text
+-------------------------------------------------------------------+
|                        BROWSER / CLIENT                           |
|                                                                   |
|   +-----------------------------------------------------------+   |
|   |                    PRESENTATION LAYER                     |   |
|   | (Standalone Components: SVG Honeycomb, Controls, End Game)|   |
|   +-----------------------------+-----------------------------+   |
|                                 |                                 |
|                                 v                                 |
|   +-----------------------------------------------------------+   |
|   |                      SERVICE LAYER                        |   |
|   |GameService (Loads dictionary.json, generates daily puzzle,|   |
|   |         extracts targetWords & Mielegrammi Sets)          |   |
|   +-----------------------------+-----------------------------+   |
|                                 |                                 |
|                 +---------------+---------------+                 |
|                 |                               |                 |
|                 v                               v                 |
|   +---------------------------+   +---------------------------+   |
|   |  IN-MEMORY TARGET SETS    |   |     PERSISTENCE LAYER     |   |
|   | (Instant O(1) Validation) |   |    (Browser LocalStorage) |   |
|   +---------------------------+   +---------------------------+   |
+-------------------------------------------------------------------+
```
---

## 2. Technology Stack & Key Decisions

| Tech / Library | Selection | Rationale |
| :--- | :--- | :--- |
| **Framework** | Angular 21 | High-performance reactive framework using modern Standalone Components and Signals. |
| **State Management** | Angular Signals | Built-in reactive primitives (`signal`, `computed`, `effect`) eliminating external state libraries. |
| **Rendering Engine** | Scalable Vector Graphics (SVG) | Native vector math for scalable, crisp hexagonal tiles across all screen resolutions. |
| **Persistence** | Browser `localStorage` | Preserves game state and progress across page reloads without backend dependency. |
| **Styling** | SCSS / Tailwind CSS | Utility-first responsive design and custom animations (e.g., tile press, error shake). |

---

## 3. Core Component Architecture

The application is decomposed into standalone, single-responsibility components:

* **`AppComponent`**: Main container managing layout shell and global keyboard event listeners (`keydown`).
* **`HeaderComponent`**: Displays game logo, score counter, and rank progression.
* **`HoneycombGridComponent`**: SVG-based component responsible for drawing the 7 hexagonal tiles and handling click/touch inputs.
* **`WordDisplayComponent`**: Displays live typed string, cursor animations, and feedback error toasts.
* **`ControlsComponent`**: Houses interaction buttons (*Delete*, *Shuffle*, *Enter*).
* **`FoundWordsPanelComponent`**: Accordion/list panel displaying all validated words discovered by the player.

---

## 4. State Management & Service Layer (`GameService`)

All business logic resides within `GameService`, providing centralized reactive state using Angular Signals:

### Key Service Responsibilities
1. **Input Handling:** Appends or deletes characters from `currentInput` signal via keyboard or UI tile clicks.
2. **Shuffle Logic:** Randomly shuffles array indices for the 6 outer `HexTile` positions while keeping index 0 (center) immutable.
3. **Validation Pipeline:** Evaluates submitted words against game rules (Length $\ge 4$, mandatory center letter, dictionary existence, duplicate check).
4. **Scoring Engine:** Calculates earned points (1 pt for 4-letter words, 1 pt per letter for $>4$ letters) and evaluates **Mielegramma** bonus (+7 pts).
5. **Auto-Save Sync:** Uses an Angular `effect()` to automatically mirror state changes into `localStorage`.

---

## 5. Performance Strategy

* **Dictionary Lookup ($O(1)$ Complexity):** The daily dictionary list is stored in a JavaScript `Set` object, guaranteeing constant-time lookup performance during word submission.
* **Event Delegation:** Global keyboard inputs are bound using standard Angular HostListeners on `window` to capture `Backspace`, `Enter`, and character inputs seamlessly.