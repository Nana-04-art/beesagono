import { WordMapItem } from "./word-map-item.model";

export interface LetterGroup {
  letter: string;
  isCenter: boolean;
  totalCount: number;
  foundCount: number;
  foundWords: string[];
  foundPangrams: number;
  totalPangrams: number;
  wordItems: WordMapItem[];
}