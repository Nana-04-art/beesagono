export interface ShareScorePayload {
    date: string;            // DD/MM/YYYY, display format
    score: number;
    maxScore: number;
    wordsFound: number;
    totalWords: number;
    mielegrammiFound: number;
    totalMielegrammi: number;
}