import { RankTier } from '../models/rank.model';

export const RANK_TIERS: readonly RankTier[] = [
    { threshold: 0, label: 'Iniziato' },
    { threshold: 2, label: 'Mente Fresca' },
    { threshold: 5, label: 'Principiante' },
    { threshold: 8, label: 'Avanzato' },
    { threshold: 15, label: 'Esperto' },
    { threshold: 25, label: 'Eccellente' },
    { threshold: 40, label: 'Genio' },
    { threshold: 70, label: 'Maestro' },
    { threshold: 100, label: 'Ape Regina' },
] as const;