import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WordMapItem } from '../../models/game/word-map-item.model';

@Component({
  selector: 'app-word-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './word-map.component.html',
  styleUrl: './word-map.component.scss',
})
export class WordMapComponent {
  items = input.required<WordMapItem[]>();
  letterColors = input.required<Map<string, string> | Record<string, string>>();

  /**
   *  Retrieves the HEX/CSS color assigned to the word's initial letter. 
   * Handles both JS Map objects and plain Record objects (JSON from API).
  */
  getLetterColor(item: WordMapItem): string {
    const initial = (item.initial || item.word?.[0] || '').toUpperCase();
    const colors = this.letterColors();

    if (!colors || !initial) {
      return '#e2e8f0';
    }

    if (colors instanceof Map) {
      return colors.get(initial) || '#e2e8f0';
    }

    return (colors as Record<string, string>)[initial] || '#e2e8f0';
  }
}