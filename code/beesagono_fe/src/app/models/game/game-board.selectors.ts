import { GameBoard } from './game-board.model';

export function getCenterLetter(board: GameBoard): string {
  return board.cells.find((c) => c.isCenter)!.letter;
}

export function getOuterLetters(board: GameBoard): string[] {
  return board.cells.filter((c) => !c.isCenter).map((c) => c.letter);
}

export function getAvailableLetters(board: GameBoard): string[] {
  return board.cells.map((c) => c.letter);
}