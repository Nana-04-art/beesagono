import { Component, input, output } from '@angular/core';
import { Cell } from '../../models/cell.model';

@Component({
  selector: 'app-honeycomb-grid',
  imports: [],
  templateUrl: './honeycomb-grid.component.html',
  styleUrl: './honeycomb-grid.component.scss',
})
export class HoneycombGridComponent {
  // Required input signal containing the display cells
  readonly cells = input.required<Cell[]>();

  // Output signal emitting the selected letter
  readonly letterTapped = output<string>();

  onCellClick(event: MouseEvent, letter: string): void {
    // Remove focus from the clicked SVG element to prevent 'Enter' key from re-triggering it
    const target = event.currentTarget as SVGElement | HTMLElement;
    if (target && typeof target.blur === 'function') {
      target.blur();
    }

    this.letterTapped.emit(letter);
  }

  onKeyDown(event: KeyboardEvent, letter: string): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();

      // Remove focus as well for keyboard interactions if desired
      const target = event.currentTarget as SVGElement | HTMLElement;
      if (target && typeof target.blur === 'function') {
        target.blur();
      }

      this.letterTapped.emit(letter);
    }
  }

  // Pure SVG helper to compute standard 2D layout coordinates for radial hexagons
  getHexCoordinates(position: number): { x: number; y: number } {
    const centerX = 160;
    const centerY = 160;

    if (position === 0) {
      return { x: centerX, y: centerY }; // Center tile coordinate
    }

    const distance = 92;

    const angleOffset = -Math.PI / 2; // Position 1 starts top-center
    const angle = angleOffset + ((position - 1) * Math.PI) / 3;

    return {
      x: centerX + distance * Math.cos(angle),
      y: centerY + distance * Math.sin(angle),
    };
  }
}