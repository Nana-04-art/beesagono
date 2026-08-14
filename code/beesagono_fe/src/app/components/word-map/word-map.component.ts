import { Component, inject, input } from '@angular/core';
import { WordMapItem } from '../../models/word-map-item.model';
import { GameService } from '../../services/game/game.service';

@Component({
  selector: 'app-word-map',
  imports: [],
  templateUrl: './word-map.component.html',
  styleUrl: './word-map.component.scss',
})
export class WordMapComponent {
  items = input.required<WordMapItem[]>();
  private gameService = inject(GameService);
  letterColors = this.gameService.letterColors;
}