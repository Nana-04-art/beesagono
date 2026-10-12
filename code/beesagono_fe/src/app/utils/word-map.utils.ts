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
 * Constructs an ordered list of WordMapItems, assigning the initial letter's color if the word has been found.
 */
export function buildWordMap(
    possibleWords: string[],
    foundWords: string[],
    mielegrammiSet: Set<string>,
    letterColorsMap?: Map<string, string>
): WordMapItem[] {
    if (!possibleWords || possibleWords.length === 0) {
        return [];
    }

    const foundSet = new Set(foundWords.map((w) => w.toUpperCase()));

    return possibleWords
        .map((word) => {
            const upperWord = word.toUpperCase();
            const initial = upperWord[0];
            const isFound = foundSet.has(upperWord);

            return {
                word: upperWord,
                length: upperWord.length,
                initial,
                isFound,
                isPangram: mielegrammiSet.has(upperWord),
                color: isFound && letterColorsMap ? letterColorsMap.get(initial) : undefined,
            };
        })
        .sort((a, b) => {
            if (a.length !== b.length) {
                return a.length - b.length;
            }
            return a.word.localeCompare(b.word);
        });
}