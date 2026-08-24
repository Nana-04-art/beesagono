import { Injectable, inject, signal } from '@angular/core';
import { StorageService } from '../storage/storage.service';

@Injectable({
  providedIn: 'root',
})
export class WelcomeNoticeService {
  private readonly storage = inject(StorageService);
  private readonly STORAGE_KEY = 'mielegrammi_welcome_disclaimer_seen';

  readonly isNoticeOpen = signal<boolean>(false);

  checkAndShowNotice(): void {
    try {
      // Attempts to load the state from StorageService / localStorage
      const hasSeen = this.storage.load<boolean | string>(this.STORAGE_KEY);
      if (!hasSeen) {
        this.isNoticeOpen.set(true);
      }
    } catch {
      // In case of SecurityError or disabled storage, show the in-memory disclaimer
      this.isNoticeOpen.set(true);
    }
  }

  dismissNotice(): void {
    try {
      this.storage.save(this.STORAGE_KEY, true);
    } catch {
      // Ignore storage write errors (e.g., Incognito/blocked cookies)
    } finally {
      this.isNoticeOpen.set(false);
    }
  }
}