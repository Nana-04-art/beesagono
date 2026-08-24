import {
  Component,
  ElementRef,
  HostListener,
  effect,
  input,
  output,
  viewChild,
} from '@angular/core';

import { ShareScorePayload } from '../../models/share-score.model'; 

@Component({
  selector: 'app-end-game-modal',
  imports: [],
  templateUrl: './end-game-modal.component.html',
  styleUrl: './end-game-modal.component.scss',
})
export class EndGameModalComponent {
  // Input signal indicating whether the modal dialog is visible
  readonly isOpen = input<boolean>(false);

  // Payload containing game stats to display and share
  readonly payload = input<ShareScorePayload | null>(null);

  // Emitted when the user closes the modal
  readonly closed = output<void>();

  // Emitted when the user clicks the share button
  readonly shareRequested = output<void>();

  // Reference to the modal dialog container
  readonly modalContainer = viewChild<ElementRef<HTMLElement>>('modalContainer');

  // Reference to the primary action button to receive initial focus
  readonly shareButton = viewChild<ElementRef<HTMLButtonElement>>('shareButton');

  // Stores the element that had focus before the modal opened
  private previouslyFocusedElement: HTMLElement | null = null;

  constructor() {
    effect(() => {
      if (this.isOpen()) {
        this.previouslyFocusedElement = document.activeElement as HTMLElement;
        setTimeout(() => {
          this.shareButton()?.nativeElement.focus();
        }, 0);
      } else {
        if (this.previouslyFocusedElement) {
          this.previouslyFocusedElement.focus();
          this.previouslyFocusedElement = null;
        }
      }
    });
  }

  // Closes the modal when the Escape key is pressed, if the modal is currently open
  @HostListener('document:keydown.escape', ['$event'])
  handleEscape(event: Event): void {
    if (this.isOpen()) {
      event.preventDefault();
      this.closeModal();
    }
  }

  // Focus Lock: Ensures that when the modal is open, focus remains trapped within the modal dialog.
  @HostListener('document:keydown.tab', ['$event'])
  handleTabTrap(event: Event): void {
    if (!this.isOpen()) return;

    const keyEvent = event as KeyboardEvent;
    const container = this.modalContainer()?.nativeElement;
    if (!container) return;

    const focusableElements = container.querySelectorAll<HTMLElement>(
      'button:not([disabled]), [tabindex]:not([tabindex="-1"])'
    );

    if (focusableElements.length === 0) return;

    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    if (keyEvent.shiftKey && document.activeElement === firstElement) {
      keyEvent.preventDefault();
      lastElement.focus();
    }

    else if (!keyEvent.shiftKey && document.activeElement === lastElement) {
      keyEvent.preventDefault();
      firstElement.focus();
    }
  }

  closeModal(): void {
    this.closed.emit();
  }

  // Handles clicking the share score button
  onShare(): void {
    this.shareRequested.emit();
  }
}