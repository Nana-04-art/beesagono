import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, input, signal, WritableSignal } from '@angular/core';
import { vi, describe, beforeEach, it, expect } from 'vitest';

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
class MockScoreboardComponent {
  score = input<number>(0);
  rankName = input<string>();
}

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

  describe('Rules Popover State', () => {
    it('should initialize isRulesOpen as false', () => {
      expect(component.isRulesOpen()).toBe(false);
    });

    it('should toggle isRulesOpen when toggleRules() is called', () => {
      component.toggleRules();
      expect(component.isRulesOpen()).toBe(true);

      component.toggleRules();
      expect(component.isRulesOpen()).toBe(false);
    });

    it('should set isRulesOpen to false when closeRules() is called', () => {
      component.isRulesOpen.set(true);
      component.closeRules();
      expect(component.isRulesOpen()).toBe(false);
    });

    it('should close rules when clicking outside the component', () => {
      component.isRulesOpen.set(true);

      const outsideElement = document.createElement('div');
      document.body.appendChild(outsideElement);

      const clickEvent = new MouseEvent('click', { bubbles: true });
      outsideElement.dispatchEvent(clickEvent);

      expect(component.isRulesOpen()).toBe(false);

      document.body.removeChild(outsideElement);
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
    it('should emit helpRequested when helpRequested.emit() is called', () => {
      let emitted = false;
      component.helpRequested.subscribe(() => (emitted = true));

      component.helpRequested.emit();

      expect(emitted).toBe(true);
    });

    it('should emit statsRequested when statsRequested.emit() is called', () => {
      let emitted = false;
      component.statsRequested.subscribe(() => (emitted = true));

      component.statsRequested.emit();

      expect(emitted).toBe(true);
    });

    it('should emit shareRequested when shareRequested.emit() is called', () => {
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