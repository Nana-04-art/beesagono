import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, input } from '@angular/core';
import { vi, describe, beforeEach, it, expect } from 'vitest';
import { HeaderComponent } from './header.component';
import { ScoreboardComponent } from '../scoreboard/scoreboard.component';

@Component({
  selector: 'app-scoreboard',
  standalone: true,
  template: ''
})
class MockScoreboardComponent {
  score = input<number>(0);
  rankName = input<string>();
}

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HeaderComponent],
    })
      .overrideComponent(HeaderComponent, {
        remove: { imports: [ScoreboardComponent] },
        add: { imports: [MockScoreboardComponent] }
      })
      .compileComponents();

    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('score', 120);
    fixture.componentRef.setInput('rank', { label: '🐝 Ape Operaia' });
    fixture.componentRef.setInput('formattedDate', '5 AGOSTO 2026');

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create the header component instance', () => {
    expect(component).toBeTruthy();
  });

  describe('Scoreboard & Rules Toggle State', () => {
    it('should initialize states as false', () => {
      expect(component.isScoreboardOpen()).toBe(false);
      expect(component.isRulesOpen()).toBe(false);
    });

    it('should toggle isScoreboardOpen when toggleScoreboard() is called', () => {
      component.toggleScoreboard();
      expect(component.isScoreboardOpen()).toBe(true);

      component.toggleScoreboard();
      expect(component.isScoreboardOpen()).toBe(false);
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

    it('should close all popovers when Escape key is pressed', () => {
      component.isScoreboardOpen.set(true);
      component.isRulesOpen.set(true);

      component.handleEscape();

      expect(component.isScoreboardOpen()).toBe(false);
      expect(component.isRulesOpen()).toBe(false);
    });
  });

  describe('Input Signals', () => {
    it('should receive inputs correctly', () => {
      expect(component.score()).toBe(120);
      expect(component.rank().label).toBe('🐝 Ape Operaia');
      expect(component.formattedDate()).toBe('5 AGOSTO 2026');
    });
  });

  describe('Outputs and Events', () => {
    it('should emit statsRequested when statsRequested.emit() is called', () => {
      const spy = vi.fn();
      component.statsRequested.subscribe(spy);

      component.statsRequested.emit();

      expect(spy).toHaveBeenCalledTimes(1);
    });

    it('should emit shareRequested when shareRequested.emit() is called', () => {
      const spy = vi.fn();
      component.shareRequested.subscribe(spy);

      component.shareRequested.emit();

      expect(spy).toHaveBeenCalledTimes(1);
    });

    it('should emit logoClicked when onLogoClick is called', () => {
      const spy = vi.fn();
      component.logoClicked.subscribe(spy);

      component.onLogoClick();

      expect(spy).toHaveBeenCalledTimes(1);
    });
  });
});