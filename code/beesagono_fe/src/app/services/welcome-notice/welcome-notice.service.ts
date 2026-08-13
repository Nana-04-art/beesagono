import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class WelcomeNoticeService {
  private readonly STORAGE_KEY = 'mielegrammi_welcome_disclaimer_seen';
  readonly isNoticeOpen = signal<boolean>(false);

  checkAndShowNotice(): void {
    const hasSeen = localStorage.getItem(this.STORAGE_KEY);
    if (!hasSeen) {
      this.isNoticeOpen.set(true);
    }
  }

  dismissNotice(): void {
    localStorage.setItem(this.STORAGE_KEY, 'true');
    this.isNoticeOpen.set(false);
  }
}