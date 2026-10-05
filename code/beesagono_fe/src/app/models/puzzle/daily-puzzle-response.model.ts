export interface DailyPuzzleResponse {
    id?: string;
    puzzleDate: string;
    centerLetter: string;
    outerLetters: string[];
    maxScore: number;
    validWords?: string[];
    mielegrammi?: string[];
}