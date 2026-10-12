export interface RankTier {
  /** Minimum percentage (0-100) required to reach this rank */
  threshold: number;
  /** Label displayed to the user (e.g. 'Genio', 'Ape Regina') */
  label: string;
}