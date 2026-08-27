import { HexPosition } from './hex-position.type';

export interface Cell {
  /** Unique ID for trackBy DOM operations (e.g., 'hex-0') */
  id: string;

  /** Uppercase letter displayed on the tile */
  letter: string;

  /** Spatial position index on the honeycomb board (0 = center). Shuffle (FR-05) only reorders positions 1-6; position 0 is immutable. */
  position: HexPosition;

  /** True if this is the central mandatory letter tile */
  isCenter: boolean;

  /** Visual state: true if currently highlighted during active input composition */
  isSelected?: boolean;

  /** Visual state: true while pressed/clicked (triggers shrink/bounce CSS) */
  isActive?: boolean;
}