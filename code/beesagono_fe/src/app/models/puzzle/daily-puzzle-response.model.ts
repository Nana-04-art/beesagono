export interface DailyPuzzleResponse {
    id?: string;
    puzzleDate: string;
    centerLetter: string;
    outerLetters: string[];
    maxScore: number;
    totalWordsCount?: number;
    totalMielegrammiCount?: number;
    wordsCountByLetter?: Record<string, number>;
    mielegrammiCountByLetter?: Record<string, number>;
}