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

  onCellClick(letter: string): void {
    this.letterTapped.emit(letter);
  }

  onKeyDown(event: KeyboardEvent, letter: string): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.letterTapped.emit(letter);
    }
  }

  // Pure SVG helper to compute standard 2D layout coordinates for radial hexagons
  getHexCoordinates(position: number): { x: number; y: number } {
    if (position === 0) {
      return { x: 150, y: 150 }; // Center tile coordinate
    }

    const radius = 82;
    const angleOffset = -Math.PI / 2; // Position 1 starts top-center
    const angle = angleOffset + ((position - 1) * Math.PI) / 3;

    return {
      x: 150 + radius * Math.cos(angle),
      y: 150 + radius * Math.sin(angle),
    };
  }
}
