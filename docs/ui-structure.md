# UI Structure & Layout Architecture — Beesagono

This document describes the user interface layout, visual hierarchy, Angular component structure, and responsive design guidelines for **Beesagono**.

---

## 1. Overall Layout Overview

The UI is organized as a single-page responsive application centered vertically and horizontally for desktop and mobile viewports.

```text
+-------------------------------------------------------+
|                    HEADER / NAVBAR                    |
| [ Logo / Title ]    [ Score / Rank ]    [ Info Modal ]|
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
+-------------------------------------------------------+
```

---

## 2. Main View Components

### A. Header Component (`HeaderComponent`)
- **Logo & Title:** Displays the game title (*Beesagono*).
- **Rank & Score Indicator:** Displays current points and player rank progression.
- **Game Info Button:** Opens modal with rules and scoring details.

### B. Input Display & Toast Component (`WordDisplayComponent`)
- **Active Input:** Live rendering of `currentInput`.
- **Feedback Toasts:** Floating overlay messages for invalid entries.
- **Shake Animation:** Triggers CSS keyframe shake on validation error.

### C. Honeycomb Grid Component (`HoneycombGridComponent`)
- **SVG Canvas:** Scalable vector graphics container rendering 7 hexagon paths.
- **Center Hexagon:** Rendered with distinct golden styling (`#f59e0b`).
- **Outer Hexagons (6x):** Neutral styling with hover/active click animations.

### D. Action Controls Component (`ControlsComponent`)
- **Delete Button:** Removes last character from input.
- **Shuffle Button:** Rotates position of the 6 outer letters.
- **Enter / Submit Button:** Validates current input string.

### E. Found Words Panel Component (`FoundWordsPanelComponent`)
- **Word Counter:** Displays total count of discovered words.
- **Word List:** Scrollable list showing guessed words.
- **Mielegramma Highlight:** Gold highlight for discovered pangrams.

---

## 3. Visual States & Color System

| State / Element | Color / Style | Purpose |
| :--- | :--- | :--- |
| **Center Hex Tile** | Gold / Amber (`#f59e0b`) | Mandatory letter |
| **Outer Hex Tile** | Light Gray / Slate (`#f1f5f9`) | Standard interactive tile |
| **Hover State** | Scale up | Desktop cursor feedback |
| **Active Click State** | Scale down | Tactile touch feedback |
| **Mielegramma Text** | Golden Glow | Pangram celebration |
| **Error Feedback** | Crimson Red (`#ef4444`) + Shake | Validation error |

---

## 4. Responsive Design Guidelines

- **Mobile First:** Stacked layout optimized for touch interaction.
- **Desktop:** Centered fixed-width viewport (max-width `500px`).
- **Touch Targets:** Minimum size of **48x48px** for all tiles and buttons.

```