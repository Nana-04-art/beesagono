import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ComponentRef, signal, WritableSignal } from '@angular/core';
import { describe, beforeEach, it, expect } from 'vitest';
import { ScoreboardComponent } from './scoreboard.component';
import { GameFacadeService } from '../../../services/game/game-facade.service';
import { RankTier } from '../../../models/game/rank.model';

class MockGameFacadeService {
  readonly maxScore: WritableSignal<number> = signal<number>(100);
  readonly score: WritableSignal<number> = signal<number>(0);
  readonly rank: WritableSignal<RankTier> = signal<RankTier>({
    label: '🐝 Ape Regina',
    threshold: 100
  });
}

describe('ScoreboardComponent', () => {
  let component: ScoreboardComponent;
  let componentRef: ComponentRef<ScoreboardComponent>;
  let fixture: ComponentFixture<ScoreboardComponent>;
  let mockGameFacade: MockGameFacadeService;

  beforeEach(async () => {
    mockGameFacade = new MockGameFacadeService();

    await TestBed.configureTestingModule({
      imports: [ScoreboardComponent],
      providers: [
        { provide: GameFacadeService, useValue: mockGameFacade }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ScoreboardComponent);
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should calculate progress percentage based on score and maxScore', () => {
    componentRef.setInput('score', 25);
    mockGameFacade.maxScore.set(100);
    fixture.detectChanges();

    expect(component.progressPercentage()).toBe(25);
  });

  it('should return 0 progress percentage if maxScore is 0', () => {
    componentRef.setInput('score', 10);
    mockGameFacade.maxScore.set(0);
    fixture.detectChanges();

    expect(component.progressPercentage()).toBe(0);
  });

  it('should compute pointsToNextRank correctly using RANK_TIERS and maxScore', () => {
    mockGameFacade.maxScore.set(100);
    componentRef.setInput('score', 20);
    fixture.detectChanges();

    const nextTier = component.nextRankTier();
    if (nextTier) {
      const expectedPoints = Math.ceil((100 * nextTier.threshold) / 100) - 20;
      expect(component.pointsToNextRank()).toBe(expectedPoints);
    } else {
      expect(component.pointsToNextRank()).toBe(0);
    }
  });

  it('should return 0 for pointsToNextRank if score exceeds or reaches maximum rank', () => {
    componentRef.setInput('score', 150);
    mockGameFacade.maxScore.set(100);
    fixture.detectChanges();

    expect(component.pointsToNextRank()).toBe(0);
  });

  it('should extract emoji from current rank label', () => {
    mockGameFacade.rank.set({ label: '🐝 Ape Regina', threshold: 100 });
    fixture.detectChanges();

    expect(component.currentRankEmoji()).toBe('🐝');
  });
});