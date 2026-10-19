import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { vi, describe, beforeEach, it, expect } from 'vitest';
import { StatsComponent } from './stats.component';
import { StatsService } from '../../../services/stats/stats.service';
import { getTodayIsoString } from '../../../utils/game-state.validator';

describe('StatsComponent', () => {
  let component: StatsComponent;
  let fixture: ComponentFixture<StatsComponent>;

  let mockStatsService: {
    stats: ReturnType<typeof signal>;
    currentTier: ReturnType<typeof signal>;
  };

  const todayStr = getTodayIsoString();

  const createMockStats = (gamesPlayed = 10, gamesCompleted = 8) => ({
    gamesPlayed,
    gamesCompleted,
    currentStreak: 3,
    maxStreak: 5,
    lastPlayedDate: todayStr,
    currentSeason: {
      year: new Date().getFullYear(),
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
    dailyRankDistribution: {
      'APPRENDISTA': 4,
      'GENIO': 2
    }
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

  it('should compute basic game counts and streaks correctly', () => {
    expect(component.gamesPlayed()).toBe(10);
    expect(component.gamesCompleted()).toBe(8);
    expect(component.currentStreak()).toBe(3);
    expect(component.maxStreak()).toBe(5);
  });

  it('should compute completion rate correctly', () => {
    expect(component.completionRate()).toBe(80);
  });

  it('should return 0 completion rate when no games played', () => {
    mockStatsService.stats.set(createMockStats(0, 0));
    fixture.detectChanges();

    expect(component.completionRate()).toBe(0);
  });

  it('should expose effectiveRankDistribution correctly', () => {
    expect(component.effectiveRankDistribution()).toEqual({
      'APPRENDISTA': 4,
      'GENIO': 2
    });
  });

  it('should fallback to empty record if dailyRankDistribution is missing', () => {
    const statsWithoutDistribution = { ...createMockStats(), dailyRankDistribution: undefined };
    mockStatsService.stats.set(statsWithoutDistribution as any);
    fixture.detectChanges();

    expect(component.effectiveRankDistribution()).toEqual({});
  });

  it('should expose currentSeason correctly', () => {
    const season = component.currentSeason();
    expect(season).toBeDefined();
    expect(season?.year).toBe(new Date().getFullYear());
    expect(season?.totalSeasonPoints).toBe(1200);
    expect(season?.highestTierAchieved).toBe('🐝 Ape Operaia');
  });

  it('should expose currentTier from statsService', () => {
    expect(component.currentTier()).toBe('🐝 Ape Operaia');
  });
});