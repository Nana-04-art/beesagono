export interface GameSyncRequest {
  puzzleDate: string; // YYYY-MM-DD
  centerLetter?: string;
  outerLetters?: string[];
  foundWords: string[];
  invalidWords?: string[];
  foundMielegrammi?: string[];
  currentScore?: number;
  currentRankLabel?: string;
  startTime?: number;
  lastUpdated?: number;
  completionPercentage?: number;
  isCompleted?: boolean;
}

export interface GameBulkSyncRequest {
  games: GameSyncRequest[];
}