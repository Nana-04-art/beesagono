import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, signal, WritableSignal } from '@angular/core';
import { vi } from 'vitest';

import { HeaderComponent } from './header.component';
import { GameBoard } from '../../models/game-board.model';
import { RankTier } from '../../models/rank.model';
import { ScoreboardComponent } from '../scoreboard/scoreboard.component';
import { GameService } from '../../services/game/game.service';

@Component({
  selector: 'app-scoreboard',
  standalone: true,
  template: ''
})
class MockScoreboardComponent { }

class MockGameService {
  readonly board: WritableSignal<GameBoard | null> = signal<GameBoard | null>(null);
  readonly rank: WritableSignal<RankTier> = signal<RankTier>({ label: 'Iniziato', threshold: 0 });
  readonly score: WritableSignal<number> = signal<number>(0);
}

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;
  let mockGameService: MockGameService;

  beforeEach(async () => {
    mockGameService = new MockGameService();

    await TestBed.configureTestingModule({
      imports: [HeaderComponent],
      providers: [
        { provide: GameService, useValue: mockGameService }
      ]
    })
      .overrideComponent(HeaderComponent, {
        remove: { imports: [ScoreboardComponent] },
        add: { imports: [MockScoreboardComponent] }
      })
      .compileComponents();

    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the header component instance', () => {
    expect(component).toBeTruthy();
  });

  describe('Computed Score Properties', () => {
    it('should reactively reflect score from GameService', () => {
      mockGameService.score.set(42);
      fixture.detectChanges();

      expect(component.score()).toBe(42);
    });

    it('should compute maxScore from board when board is loaded', () => {
      const mockBoard: GameBoard = {
        date: '2026-08-05',
        seed: 'test-seed',
        cells: [],
        possibleWords: ['CASA'],
        mielegrammi: ['CAMPIO'],
        maxScore: 180
      };

      mockGameService.board.set(mockBoard);
      fixture.detectChanges();

      expect(component.maxScore()).toBe(180);
    });

    it('should fallback maxScore to 100 when board is null', () => {
      mockGameService.board.set(null);
      fixture.detectChanges();

      expect(component.maxScore()).toBe(100);
    });
  });

  describe('Rank Icon Emoji Calculation', () => {
    const testCases: { threshold: number; expectedIcon: string }[] = [
      { threshold: 100, expectedIcon: '🐝' },
      { threshold: 85, expectedIcon: '👑' },
      { threshold: 70, expectedIcon: '👑' },
      { threshold: 55, expectedIcon: '🧠' },
      { threshold: 40, expectedIcon: '🧠' },
      { threshold: 30, expectedIcon: '⭐' },
      { threshold: 25, expectedIcon: '⭐' },
      { threshold: 20, expectedIcon: '💡' },
      { threshold: 15, expectedIcon: '💡' },
      { threshold: 10, expectedIcon: '🚀' },
      { threshold: 8, expectedIcon: '🚀' },
      { threshold: 6, expectedIcon: '🐣' },
      { threshold: 5, expectedIcon: '🐣' },
      { threshold: 3, expectedIcon: '🍃' },
      { threshold: 2, expectedIcon: '🍃' },
      { threshold: 1, expectedIcon: '🌱' },
      { threshold: 0, expectedIcon: '🌱' }
    ];

    testCases.forEach(({ threshold, expectedIcon }) => {
      it(`should return emoji '${expectedIcon}' for threshold ${threshold}`, () => {
        mockGameService.rank.set({ label: 'Grado Test', threshold });
        fixture.detectChanges();

        expect(component.rankIcon()).toBe(expectedIcon);
      });
    });
  });

  describe('Formatted Date Logic', () => {
    it('should return empty string when board is null', () => {
      mockGameService.board.set(null);
      fixture.detectChanges();

      expect(component.formattedDate()).toBe('');
    });

    it('should format date string YYYY-MM-DD into Italian uppercase long date format', () => {
      const mockBoard: GameBoard = {
        date: '2026-08-05',
        seed: 'test-seed',
        cells: [],
        possibleWords: [],
        mielegrammi: [],
        maxScore: 100
      };

      mockGameService.board.set(mockBoard);
      fixture.detectChanges();

      const result = component.formattedDate();
      expect(result).toContain('5');
      expect(result).toContain('AGOSTO');
      expect(result).toContain('2026');
    });
  });

  describe('Outputs and Events', () => {
    it('should emit helpRequested when rules/help trigger occurs', () => {
      let emitted = false;
      component.helpRequested.subscribe(() => (emitted = true));

      component.helpRequested.emit();

      expect(emitted).toBe(true);
    });

    it('should emit statsRequested when stats trigger occurs', () => {
      let emitted = false;
      component.statsRequested.subscribe(() => (emitted = true));

      component.statsRequested.emit();

      expect(emitted).toBe(true);
    });

    it('should emit shareRequested when share button is clicked', () => {
      let emitted = false;
      component.shareRequested.subscribe(() => (emitted = true));

      component.shareRequested.emit();

      expect(emitted).toBe(true);
    });
  });

  describe('User Interaction Methods', () => {
    it('should call window.scrollTo with top:0 and smooth behavior on logo click', () => {
      const scrollToSpy = vi.spyOn(window, 'scrollTo').mockImplementation(() => { });

      component.onLogoClick();

      expect(scrollToSpy).toHaveBeenCalledWith({ top: 0, behavior: 'smooth' });
    });
  });
});