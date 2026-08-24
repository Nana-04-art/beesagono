import { Component, input } from '@angular/core';

@Component({
  selector: 'app-word-display',
  imports: [],
  templateUrl: './word-display.component.html',
  styleUrl: './word-display.component.scss',
})
export class WordDisplayComponent {
  // Currently typed input word
  readonly currentInput = input.required<string>();

  // Center letter used for visual highlight
  readonly centerLetter = input<string>('');

  // Optional feedback message
  readonly feedbackMessage = input<string | null>(null);

  // Allow null to match HiveViewComponent signal state
  readonly feedbackType = input<'error' | 'success' | 'info' | null>(null);
}
