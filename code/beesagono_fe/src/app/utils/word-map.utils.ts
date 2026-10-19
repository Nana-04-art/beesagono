import { GameBoard } from '../models/game/game-board.model';
import { WordMapItem } from '../models/game/word-map-item.model';

export const LETTER_PALETTE = [
    '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4',
    '#FFEEAD', '#D4A5A5', '#9B59B6'
];

/**
 * Maps each letter on the game board to a unique color from the palette.
 */
export function buildLetterColorsMap(board: GameBoard | null): Map<string, string> {
    const map = new Map<string, string>();
    if (!board) return map;

    board.cells.forEach((cell, index) => {
        map.set(cell.letter.toUpperCase(), LETTER_PALETTE[index % LETTER_PALETTE.length]);
    });
    return map;
}

/**
 * Constructs an ordered list of WordMapItems from found words safely.
 */
export function buildWordMapForLetter(
    letter: string,
    totalCount: number,
    foundWordsForLetter: string[]
): WordMapItem[] {
    const items: WordMapItem[] = [];
    const upperLetter = letter.toUpperCase();
    const sortedFound = [...foundWordsForLetter].sort();

    // Aggiunge prima le parole trovate
    sortedFound.forEach((word) => {
        const upperWord = word.toUpperCase();
        const isPangram = upperWord.length >= 7 && new Set(upperWord).size === 7;
        items.push({
            word: upperWord,
            length: upperWord.length,
            initial: upperLetter,
            isFound: true,
            isPangram
        });
    });

    // Aggiunge i segnaposto grigi per le parole non ancora trovate fino a raggiungere il totalCount
    const missingCount = Math.max(0, totalCount - sortedFound.length);
    for (let i = 0; i < missingCount; i++) {
        items.push({
            word: `HIDDEN_${upperLetter}_${i}`,
            length: 4, // Lunghezza indicativa per il segnaposto
            initial: upperLetter,
            isFound: false,
            isPangram: false
        });
    }

    return items;
}