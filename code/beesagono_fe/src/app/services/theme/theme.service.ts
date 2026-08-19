import { Injectable, signal, effect, inject } from '@angular/core';
import { StorageService } from '../storage/storage.service';

export type Theme = 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private storageService = inject(StorageService);
  private readonly THEME_KEY = 'user_theme';

  readonly currentTheme = signal<Theme>('light');

  constructor() {
    // Load the saved preference from the StorageService
    const savedTheme = this.storageService.load<Theme>(this.THEME_KEY);

    if (savedTheme) {
      this.currentTheme.set(savedTheme);
    } else {
      // Fallback to the operating system theme
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      this.currentTheme.set(prefersDark ? 'dark' : 'light');
    }

    // Applying the data-theme attribute to the HTML element and saving
    effect(() => {
      const theme = this.currentTheme();
      document.documentElement.setAttribute('data-theme', theme);
      this.storageService.save(this.THEME_KEY, theme);
    });
  }

  toggleTheme(): void {
    this.currentTheme.update((prev) => (prev === 'light' ? 'dark' : 'light'));
  }
}