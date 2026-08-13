import { Injectable, signal, Signal } from '@angular/core';
import { GAME_RULES } from '../../config/game-rules.config';
import { RankTier } from '../../models/rank.model';
import { RANK_TIERS } from '../../config/rank-tiers.config';

@Injectable({
  providedIn: 'root',
})
export class ScoreService {
  /** Internal signal holding total daily points scored */
  private readonly _dailyScore = signal<number>(0);

  /** Readonly signal for current daily score */
  readonly dailyScore: Signal<number> = this._dailyScore.asReadonly();

  /**
   * Calculates points awarded for a single valid word.
   * - 4-letter words = 1 point
   * - >4-letter words = 1 point per letter
   * - Mielegramma = standard points + bonus
   */
  calculateWordPoints(word: string, isMielegramma: boolean): number {
    const cleanWord = word.trim().toUpperCase();
    let points = cleanWord.length === 4 ? 1 : cleanWord.length;

    if (isMielegramma) {
      points += GAME_RULES.MIELEGRAMMA_BONUS;
    }

    return points;
  }

  /**
   * Calculates total accumulated points for an array of found words.
   */
  calculateTotalScore(foundWords: string[], mielegrammiSet: Set<string>): number {
    return foundWords.reduce((total, word) => {
      const isMielegramma = mielegrammiSet.has(word);
      return total + this.calculateWordPoints(word, isMielegramma);
    }, 0);
  }

  /**
   * Returns the corresponding RankTier based on percentage achieved relative to max score.
   */
  getRankForPercentage(percentage: number): RankTier {
    const reversedTiers = [...RANK_TIERS].reverse();
    const currentRank = reversedTiers.find((tier) => percentage >= tier.threshold);
    return currentRank ?? RANK_TIERS[0];
  }

  /**
   * Sets or updates the current daily score.
   */
  setDailyScore(score: number): void {
    this._dailyScore.set(Math.max(0, score));
  }

  /**
   * Resets daily score session (e.g. on new game load / midnight rollover).
   */
  resetDailyScore(): void {
    this._dailyScore.set(0);
  }
}