import { Component, effect, ElementRef, HostListener, inject, ViewChild } from '@angular/core';
import { WelcomeNoticeService } from '../../services/welcome-notice/welcome-notice.service';

@Component({
  selector: 'app-welcome-modal',
  imports: [],
  templateUrl: './welcome-modal.component.html',
  styleUrl: './welcome-modal.component.scss',
})
export class WelcomeModalComponent {
  readonly noticeService = inject(WelcomeNoticeService);

  @ViewChild('confirmBtn') confirmBtn?: ElementRef<HTMLButtonElement>;

  constructor() {
    // Reacts to the modal state to block or restore page scrolling
    effect(() => {
      if (this.noticeService.isNoticeOpen()) {
        document.body.style.overflow = 'hidden';
        this.focusConfirmButton();
      } else {
        document.body.style.overflow = '';
      }
    });
  }

  dismissNotice(): void {
    document.body.style.overflow = '';
    this.noticeService.dismissNotice();
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent): void {
    if (!this.noticeService.isNoticeOpen()) return;

    if (event.key === 'Tab') {
      event.preventDefault();
      this.focusConfirmButton();
    }

    if (event.key === 'Escape') {
      event.preventDefault();
      this.dismissNotice();
    }
  }

  private focusConfirmButton(): void {
    setTimeout(() => {
      this.confirmBtn?.nativeElement.focus();
    }, 50);
  }
}