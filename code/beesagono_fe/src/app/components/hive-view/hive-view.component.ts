import { Component, OnInit, inject, HostListener, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameService } from '../../services/game/game.service';
import { HoneycombGridComponent } from '../honeycomb-grid/honeycomb-grid.component';
import { WordDisplayComponent } from '../word-display/word-display.component';
import { HiveControlsComponent } from '../hive-controls/hive-controls.component';
import { ScoreboardComponent } from '../scoreboard/scoreboard.component';
import { FoundWordsComponent } from '../found-words/found-words.component';
import { EndGameModalComponent } from '../end-game-modal/end-game-modal.component';
import { ShareScorePayload } from '../../models/share-score.model';
import { HeaderComponent } from '../header/header.component';
import { WordsByLetterComponent } from '../words-by-letter/words-by-letter.component';
import { InvalidWordsComponent } from '../invalid-words/invalid-words.component';

@Component({
  selector: 'app-hive-view',
  standalone: true,
  imports: [
    CommonModule,
    HoneycombGridComponent,
    WordDisplayComponent,
    HiveControlsComponent,
    FoundWordsComponent,
    EndGameModalComponent,
    HeaderComponent,
    WordsByLetterComponent,
    InvalidWordsComponent
  ],
  templateUrl: './hive-view.component.html',
  styleUrl: './hive-view.component.scss',
})
export class HiveViewComponent implements OnInit {
  protected readonly gameService = inject(GameService);

  // Signal controlling end game modal visibility 
  protected readonly isEndGameModalOpen = signal<boolean>(false);
  protected readonly isHelpModalOpen = signal<boolean>(false);
  protected readonly isStatsModalOpen = signal<boolean>(false);

  // Computed payload for EndGameModal delegated to GameService
  protected readonly endGamePayload = computed<ShareScorePayload>(() => {
    return this.gameService.getShareScorePayload();
  });

  protected readonly feedbackMessage = signal<string>('');
  protected readonly feedbackType = signal<'error' | 'success' | null>(null);

  // Computed signal to extract the center letter from the active board
  protected readonly centerLetter = computed(() => {
    const board = this.gameService.board();
    if (!board) return '';
    const centerCell = board.cells.find((c) => c.isCenter);
    return centerCell ? centerCell.letter : '';
  });

  private feedbackTimeout?: ReturnType<typeof setTimeout>;

  ngOnInit(): void {
    if (this.gameService.loadStatus() === 'idle') {
      this.gameService.loadDailyGame();
    } else {
      this.gameService.checkDateRollover();
    }
  }

  /**
   * Checks for date rollover whenever the window regains focus or becomes visible.
   */
  @HostListener('window:focus')
  @HostListener('document:visibilitychange')
  onWindowFocusOrVisibilityChange(): void {
    if (typeof document !== 'undefined' && document.visibilityState === 'visible') {
      this.gameService.checkDateRollover();
    }
  }

  // Opens the instructions/help modal dialog
  openHelpModal(): void {
    this.isHelpModalOpen.set(true);
  }

  // Opens the player statistics modal dialog
  openStatsModal(): void {
    this.isStatsModalOpen.set(true);
  }

  // Opens the end game victory modal
  openEndGame(): void {
    this.isEndGameModalOpen.set(true);
  }

  // Closes the end game victory modal
  closeEndGame(): void {
    this.isEndGameModalOpen.set(false);
  }

  // Copies formatted game results to the user clipboard
  shareResults(): void {
    const payload = this.endGamePayload();
    const text = `🐝 Beesagono (${payload.date})\nPunti: ${payload.score}/${payload.maxScore}\nParole: ${payload.wordsFound}/${payload.totalWords}\nMielegrammi: ${payload.mielegrammiFound}/${payload.totalMielegrammi}`;

    if (navigator.clipboard) {
      navigator.clipboard.writeText(text);
      this.feedbackType.set('success');
      this.feedbackMessage.set('Risultati copiati negli appunti!');
      setTimeout(() => this.clearFeedback(), 2000);
    }
  }

  submit(): void {
    const result = this.gameService.submitWord();

    if (this.feedbackTimeout) {
      clearTimeout(this.feedbackTimeout);
    }

    if (!result.isValid) {
      this.feedbackType.set('error');
      this.feedbackMessage.set(result.message || 'Parola non valida');

      this.feedbackTimeout = setTimeout(() => {
        this.clearFeedback();
      }, 2000);
    } else {
      this.feedbackType.set('success');
      this.feedbackMessage.set(
        result.isMielegramma ? '🎉 MIELEGRAMMA!' : 'Ottimo!'
      );

      // Auto-open modal if all words are found
      if (this.gameService.isCompleted()) {
        setTimeout(() => {
          this.openEndGame();
        }, 600);
      }

      this.feedbackTimeout = setTimeout(() => {
        this.clearFeedback();
      }, 1500);
    }
  }

  @HostListener('window:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent): void {
    if (this.gameService.loadStatus() !== 'ready') return;

    if (event.key === 'Enter') {
      this.submit();
    } else if (event.key === 'Backspace') {
      this.gameService.deleteLastChar();
    } else if (/^[a-zA-Z]$/.test(event.key)) {
      this.gameService.handleInput(event.key);
    }
  }

  private clearFeedback(): void {
    this.feedbackMessage.set('');
    this.feedbackType.set(null);
  }
}