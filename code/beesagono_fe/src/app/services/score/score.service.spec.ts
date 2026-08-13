import { TestBed } from '@angular/core/testing';
import { describe, beforeEach, it, expect } from 'vitest';
import { ScoreService } from './score.service';
import { RANK_TIERS } from '../../config/rank-tiers.config';
import { GAME_RULES } from '../../config/game-rules.config';

describe('ScoreService', () => {
  let service: ScoreService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ScoreService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should calculate 1 point for 4-letter words', () => {
    const points = service.calculateWordPoints('CASA', false);
    expect(points).toBe(1);
  });

  it('should calculate letter-length points for words longer than 4 letters', () => {
    const points = service.calculateWordPoints('ALBERO', false);
    expect(points).toBe(6);
  });

  it('should add mielegramma bonus when word is a mielegramma', () => {
    const points = service.calculateWordPoints('MIELEGRAMMA', true);
    const expectedPoints = 'MIELEGRAMMA'.length + GAME_RULES.MIELEGRAMMA_BONUS;
    expect(points).toBe(expectedPoints);
  });

  it('should calculate total score correctly for a list of found words', () => {
    const foundWords = ['CASA', 'ALBERO']; // 1 + 6 = 7 points
    const mielegrammiSet = new Set<string>();

    const totalScore = service.calculateTotalScore(foundWords, mielegrammiSet);
    expect(totalScore).toBe(7);
  });

  it('should return correct rank based on percentage achieved', () => {
    const lowestTier = RANK_TIERS[0];
    const rankForZero = service.getRankForPercentage(0);
    expect(rankForZero.label).toBe(lowestTier.label);

    const highestTier = RANK_TIERS[RANK_TIERS.length - 1];
    const rankForMax = service.getRankForPercentage(100);
    expect(rankForMax.label).toBe(highestTier.label);
  });

  it('should set and reset daily score properly via signals', () => {
    expect(service.dailyScore()).toBe(0);

    service.setDailyScore(50);
    expect(service.dailyScore()).toBe(50);

    service.setDailyScore(-10);
    expect(service.dailyScore()).toBe(0);

    service.setDailyScore(25);
    service.resetDailyScore();
    expect(service.dailyScore()).toBe(0);
  });
});