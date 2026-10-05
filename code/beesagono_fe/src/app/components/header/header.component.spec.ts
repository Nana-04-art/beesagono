import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, input, output, signal } from '@angular/core';
import { vi, describe, beforeEach, it, expect } from 'vitest';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { HeaderComponent } from './header.component';
import { ScoreboardComponent } from './scoreboard/scoreboard.component';
import { StatsComponent } from './stats/stats.component';
import { RulesComponent } from './rules/rules.component';
import { ThemeService } from '../../services/theme/theme.service';
import { AuthService } from '../../services/auth/auth.service';

@Component({
  selector: 'app-scoreboard',
  standalone: true,
  template: '<div data-testid="scoreboard-mock">Scoreboard</div>',
})
class MockScoreboardComponent {
  score = input<number>(0);
  rankName = input<string>('');
  close = output<void>();
}

@Component({
  selector: 'app-stats',
  standalone: true,
  template: '<div data-testid="stats-mock">Stats</div>',
})
class MockStatsComponent {
  close = output<void>();
  shareRequested = output<void>();
}

@Component({
  selector: 'app-rules',
  standalone: true,
  template: '<div data-testid="rules-mock">Rules</div>',
})
class MockRulesComponent {
  close = output<void>();
}

class MockThemeService {
  currentTheme = signal<'light' | 'dark'>('light');
  isDarkMode = vi.fn().mockReturnValue(false);
  toggleTheme = vi.fn();
}

class MockAuthService {
  currentUser = signal<{ username: string } | null>(null);
  isLoggedIn = signal<boolean>(false);
  logout = vi.fn();
}

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;
  let themeServiceMock: MockThemeService;
  let authServiceMock: MockAuthService;

  beforeEach(async () => {
    themeServiceMock = new MockThemeService();
    authServiceMock = new MockAuthService();

    TestBed.configureTestingModule({
      imports: [HeaderComponent],
      providers: [
        provideRouter([]),
        { provide: ThemeService, useValue: themeServiceMock },
        { provide: AuthService, useValue: authServiceMock },
      ],
    });

    TestBed.overrideComponent(HeaderComponent, {
      remove: {
        imports: [ScoreboardComponent, StatsComponent, RulesComponent],
      },
      add: {
        imports: [MockScoreboardComponent, MockStatsComponent, MockRulesComponent],
      },
    });

    await TestBed.compileComponents();

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

  describe('Template Regression Tests', () => {
    it('should not have overflow-hidden class on container-fluid to prevent clipping popovers', () => {
      const containerEl = fixture.nativeElement.querySelector('.container-fluid');
      expect(containerEl).toBeTruthy();
      expect(containerEl.classList.contains('overflow-hidden')).toBe(false);
    });
  });

  describe('Input Signals', () => {
    it('should receive inputs correctly', () => {
      expect(component.score()).toBe(120);
      expect(component.rank().label).toBe('🐝 Ape Operaia');
      expect(component.formattedDate()).toBe('5 AGOSTO 2026');
    });

    it('should fall back to default input values', () => {
      const defaultFixture = TestBed.createComponent(HeaderComponent);
      const defaultComponent = defaultFixture.componentInstance;

      expect(defaultComponent.score()).toBe(0);
      expect(defaultComponent.rank()).toEqual({ label: '🌱 Iniziato' });
      expect(defaultComponent.formattedDate()).toBe('');
    });
  });

  describe('Popover State & Mutually Exclusive Toggles', () => {
    it('should initialize activePopover as null', () => {
      expect(component.activePopover()).toBeNull();
    });

    it('should toggle scoreboard popover when toggleScoreboard() is called', () => {
      component.toggleScoreboard();
      expect(component.activePopover()).toBe('scoreboard');

      component.toggleScoreboard();
      expect(component.activePopover()).toBeNull();
    });

    it('should toggle rules popover when toggleRules() is called', () => {
      component.toggleRules();
      expect(component.activePopover()).toBe('rules');

      component.toggleRules();
      expect(component.activePopover()).toBeNull();
    });

    it('should toggle stats popover when toggleStats() is called', () => {
      component.toggleStats();
      expect(component.activePopover()).toBe('stats');

      component.toggleStats();
      expect(component.activePopover()).toBeNull();
    });

    it('should toggle userMenu popover when toggleUserMenu() is called', () => {
      component.toggleUserMenu();
      expect(component.activePopover()).toBe('userMenu');

      component.toggleUserMenu();
      expect(component.activePopover()).toBeNull();
    });

    it('should ensure popovers are mutually exclusive when toggled sequentially', () => {
      component.toggleScoreboard();
      expect(component.activePopover()).toBe('scoreboard');

      component.toggleRules();
      expect(component.activePopover()).toBe('rules');

      component.toggleStats();
      expect(component.activePopover()).toBe('stats');

      component.toggleUserMenu();
      expect(component.activePopover()).toBe('userMenu');
    });

    it('should close popovers when closeAll() is called', () => {
      component.toggleRules();
      expect(component.activePopover()).toBe('rules');

      component.closeAll();
      expect(component.activePopover()).toBeNull();
    });

    it('should close all popovers when HostListener catches Escape key', () => {
      component.toggleScoreboard();
      expect(component.activePopover()).toBe('scoreboard');

      component.handleEscape();

      expect(component.activePopover()).toBeNull();
    });

    it('should trigger handleEscape via real DOM Escape keydown event', () => {
      component.toggleStats();
      expect(component.activePopover()).toBe('stats');

      const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape', code: 'Escape' });
      document.dispatchEvent(escapeEvent);

      expect(component.activePopover()).toBeNull();
    });
  });

  describe('Auth & User Menu Functionality', () => {
    it('should display login button when user is not logged in', () => {
      authServiceMock.isLoggedIn.set(false);
      fixture.detectChanges();

      const loginBtn = fixture.nativeElement.querySelector('a[title="Accedi"]');
      expect(loginBtn).toBeTruthy();
    });

    it('should display user menu button when user is logged in', () => {
      authServiceMock.isLoggedIn.set(true);
      authServiceMock.currentUser.set({ username: 'Mario' });
      fixture.detectChanges();

      const userBtn = fixture.nativeElement.querySelector('button[title="Profilo utente"]');
      expect(userBtn).toBeTruthy();
      expect(userBtn.textContent).toContain('Mario');
    });

    it('should open user menu popover and invoke logout when Esci button is clicked', () => {
      authServiceMock.isLoggedIn.set(true);
      authServiceMock.currentUser.set({ username: 'Mario' });
      fixture.detectChanges();

      component.toggleUserMenu();
      fixture.detectChanges();

      const logoutBtn = fixture.nativeElement.querySelector('.popover-dropdown button');
      expect(logoutBtn).toBeTruthy();
      expect(logoutBtn.textContent).toContain('Esci');

      logoutBtn.click();

      expect(authServiceMock.logout).toHaveBeenCalledTimes(1);
      expect(component.activePopover()).toBeNull();
    });
  });

  describe('Outputs and Event Emitters', () => {
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

  describe('Dependency Injection & Services', () => {
    it('should inject ThemeService and AuthService correctly', () => {
      expect(component.themeService).toBeDefined();
      expect(component.authService).toBeDefined();
    });
  });
});