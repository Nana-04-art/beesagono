import { CareerTier } from '../models/stats.model';

export const CAREER_TIERS: CareerTier[] = [
    { name: 'Uovo d\'Ape', minPercentage: 0 },
    { name: 'Larva', minPercentage: 15 },
    { name: 'Ape Nutrice', minPercentage: 30 },
    { name: 'Ape Operaia', minPercentage: 45 },
    { name: 'Ape Bottinatrice', minPercentage: 60 },
    { name: 'Ape Custode', minPercentage: 75 },
    { name: 'Ape Guardiana', minPercentage: 85 },
    { name: 'Ape Architetto', minPercentage: 95 },
    { name: 'Ape Regina della Stagione', minPercentage: 100 },
];

export const STREAK_MILESTONES: Record<number, number> = {
    3: 50,
    7: 150,
    15: 350,
    30: 800,
    50: 1500,
    100: 3500,
    200: 8000,
    365: 20000,
};