import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { vi, describe, beforeEach, it, expect } from 'vitest';
import { StatsComponent } from './stats.component';
import { StatsService } from '../../../services/stats/stats.service';

describe('StatsComponent', () => {
  let component: StatsComponent;
  let fixture: ComponentFixture<StatsComponent>;

  let mockStatsService: {
    stats: ReturnType<typeof signal>;
    currentTier: ReturnType<typeof signal>;
  };

  const createMockStats = (gamesPlayed = 10, gamesCompleted = 8) => ({
    gamesPlayed,
    gamesCompleted,
    currentStreak: 3,
    maxStreak: 5,
    lastPlayedDate: '2026-09-29',
    currentSeason: {
      year: 2026,
      basePointsEarned: 1000,
      bonusStreakPoints: 200,
      totalSeasonPoints: 1200,
      highestTierAchieved: '🐝 Ape Operaia',
      claimedStreakMilestones: [],
      _lastRecordedDailyScore: 0,
      _isCompletedToday: false,
      _lastRecordedRankToday: null
    },
    seasonHistory: {},
    dailyRankDistribution: {}
  });

  beforeEach(async () => {
    mockStatsService = {
      stats: signal(createMockStats(10, 8)),
      currentTier: signal('🐝 Ape Operaia'),
    };

    await TestBed.configureTestingModule({
      imports: [StatsComponent],
      providers: [
        { provide: StatsService, useValue: mockStatsService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StatsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should compute completion rate correctly', () => {
    expect(component.completionRate()).toBe(80);
  });

  it('should return 0 completion rate when no games played', () => {
    mockStatsService.stats.set(createMockStats(0, 0));
    fixture.detectChanges();

    expect(component.completionRate()).toBe(0);
  });
});