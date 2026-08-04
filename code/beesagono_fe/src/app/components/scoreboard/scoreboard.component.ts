import { Component, input } from '@angular/core';

@Component({
  selector: 'app-scoreboard',
  imports: [],
  templateUrl: './scoreboard.component.html',
  styleUrl: './scoreboard.component.scss',
})
export class ScoreboardComponent {
  // Current player score
  readonly score = input<number>(0);

  // Current Level/Rank (es. "Inizio", "Buono", "Genio")
  readonly rankName = input<string>('Inizio');

  // Points needed to reach the next level
  readonly nextRankScore = input<number>(10);

  // Percentage of completion towards the next level (0-100)
  get progressPercentage(): number {
    const max = this.nextRankScore();
    if (max <= 0) return 100;
    const current = this.score();
    return Math.min(100, Math.max(0, Math.round((current / max) * 100)));
  }
}
