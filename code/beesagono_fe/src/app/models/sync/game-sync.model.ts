export interface GameSyncRequest {
  puzzleDate: string; // YYYY-MM-DD
  centerLetter?: string;
  foundWords: string[];
  invalidWords?: string[];
  foundMielegrammi?: string[];
  completionPercentage?: number;
}

export interface GameBulkSyncRequest {
  games: GameSyncRequest[];
}