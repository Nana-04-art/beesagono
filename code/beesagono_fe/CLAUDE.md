# Instructions for Claude / AI Assistants

This project is a Greenfield Front-End application built with **Angular** for a hexagonal grid game (*Beesagono*).

## Project Guidelines & Best Practices
- **Angular Features**: Always use Standalone Components (`standalone: true`), modern Control Flow (`@if`, `@for`, `@switch`), and Angular Signals (`signal()`, `computed()`, `effect()`) for state management.
- **No Backend**: This is a pure client-side application. Use `localStorage` for persistence if needed.
- **SVG & Canvas**: Visual components use SVG element binding (`g[app-hexagon]`, dynamic `<polygon>` points).
- **Styling**: SCSS for component styling. Keep CSS clean and responsive.
- **Architecture**: Keep game logic in `GameService` and maintain components presentation-focused.

## Project Commands
- Run app: `ng serve`
- Generate component: `ng g c components/<name>`
- Generate service: `ng g s services/<name>`
