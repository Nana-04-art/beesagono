import { TestBed } from '@angular/core/testing';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { ThemeService, Theme } from './theme.service';
import { StorageService } from '../storage/storage.service';

describe('ThemeService', () => {
  let service: ThemeService;
  let mockStorageService: {
    load: ReturnType<typeof vi.fn>;
    save: ReturnType<typeof vi.fn>;
  };
  let matchMediaMock: ReturnType<typeof vi.fn>;

  const setupMatchMedia = (matches: boolean) => {
    matchMediaMock = vi.fn().mockImplementation((query: string) => ({
      matches,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    }));
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      configurable: true,
      value: matchMediaMock,
    });
  };

  beforeEach(() => {
    mockStorageService = {
      load: vi.fn(),
      save: vi.fn(),
    };

    setupMatchMedia(false);

    TestBed.configureTestingModule({
      providers: [
        ThemeService,
        { provide: StorageService, useValue: mockStorageService },
      ],
    });
  });

  afterEach(() => {
    document.documentElement.removeAttribute('data-theme');
  });

  it('should be created', () => {
    service = TestBed.inject(ThemeService);
    expect(service).toBeTruthy();
  });

  describe('Initialization Behavior', () => {
    it('should initialize with theme from StorageService if available', () => {
      mockStorageService.load.mockReturnValue('dark');

      service = TestBed.inject(ThemeService);

      expect(mockStorageService.load).toHaveBeenCalledWith('user_theme');
      expect(service.currentTheme()).toBe('dark');
    });

    it('should fall back to light theme when no stored theme and OS prefers light', () => {
      mockStorageService.load.mockReturnValue(null);
      setupMatchMedia(false);

      service = TestBed.inject(ThemeService);

      expect(service.currentTheme()).toBe('light');
    });

    it('should fall back to dark theme when no stored theme and OS prefers dark', () => {
      mockStorageService.load.mockReturnValue(null);
      setupMatchMedia(true);

      service = TestBed.inject(ThemeService);

      expect(service.currentTheme()).toBe('dark');
    });
  });

  describe('Theme Toggling & DOM Side Effects', () => {
    it('should toggle theme from light to dark and vice versa', () => {
      mockStorageService.load.mockReturnValue('light');
      service = TestBed.inject(ThemeService);

      expect(service.currentTheme()).toBe('light');

      service.toggleTheme();
      expect(service.currentTheme()).toBe('dark');

      service.toggleTheme();
      expect(service.currentTheme()).toBe('light');
    });

    it('should update DOM data-theme attribute and save to StorageService via effect', () => {
      mockStorageService.load.mockReturnValue('light');
      service = TestBed.inject(ThemeService);

      TestBed.flushEffects();

      expect(document.documentElement.getAttribute('data-theme')).toBe('light');
      expect(mockStorageService.save).toHaveBeenCalledWith('user_theme', 'light');

      service.toggleTheme();
      TestBed.flushEffects();

      expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
      expect(mockStorageService.save).toHaveBeenCalledWith('user_theme', 'dark');
    });
  });
});