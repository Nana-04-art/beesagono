import { Component, computed, inject, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameService } from '../../../services/game/game.service';
import { ScoreService } from '../../../services/score/score.service';
import { RANK_TIERS } from '../../../config/rank-tiers.config';

@Component({
  selector: 'app-scoreboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './scoreboard.component.html',
  styleUrl: './scoreboard.component.scss',
})
export class ScoreboardComponent {
  private readonly gameService = inject(GameService);
  private readonly scoreService = inject(ScoreService);

  // Inputs received from the parent (with fallback to GameService signals)
  scoreInput = input<number | undefined>(undefined, { alias: 'score' });
  rankNameInput = input<string | undefined>(undefined, { alias: 'rankName' });

  // Actual score
  score = computed(() => {
    const inputVal = this.scoreInput();
    return inputVal !== undefined ? inputVal : this.gameService.score();
  });

  // Actual rank name
  rankName = computed(() => {
    const inputVal = this.rankNameInput();
    return inputVal !== undefined ? inputVal : this.gameService.rank().label;
  });

  maxScore = computed(() => this.gameService.maxScore());

  // Calculation of the fill percentage (0% - 100%)
  progressPercentage = computed(() =>
    this.scoreService.calculatePercentage(this.score(), this.maxScore())
  );

  // Find the next level to reach
  nextRankTier = computed(() =>
    this.scoreService.getNextRank(this.score(), this.maxScore())
  );

  // Points needed to reach the next rank (calculated based on nextRankTier)
  pointsToNextRank = computed(() =>
    this.scoreService.getPointsToNext(this.score(), this.maxScore())
  );

  // Current rank emoji
  currentRankEmoji = computed(() => {
    const label = this.rankName();
    return label ? label.split(' ')[0] : '🐝';
  });

  nextRankEmoji = computed(() => {
    const next = this.nextRankTier();
    return next ? next.label.split(' ')[0] : null;
  });

  nextRankThreshold = computed(() => {
    const next = this.nextRankTier();
    return next ? next.threshold : 100;
  });

  // Map of all ranks with required points and unlocked status
  rankTiersWithPoints = computed(() => {
    const max = this.maxScore();
    const currentScore = this.score();

    return RANK_TIERS.map((tier) => {
      const requiredPoints = Math.ceil((max * tier.threshold) / 100);
      return {
        ...tier,
        requiredPoints,
        isUnlocked: currentScore >= requiredPoints,
        isCurrent: this.rankName().includes(tier.label.split(' ').slice(1).join(' ')) ||
          this.rankName() === tier.label,
      };
    });
  });
}