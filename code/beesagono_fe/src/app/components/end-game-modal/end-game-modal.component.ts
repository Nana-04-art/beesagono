import { Component, effect, input, output, viewChild } from '@angular/core';
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

  // Handles closing the modal
  closeModal(): void {
    this.closed.emit();
  }

  // Handles clicking the share score button
  onShare(): void {
    this.shareRequested.emit();
  }
}