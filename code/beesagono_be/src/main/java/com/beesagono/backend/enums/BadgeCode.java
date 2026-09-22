package com.beesagono.backend.enums;

import lombok.Getter;

@Getter
public enum BadgeCode {

        /*
         * CATEGORY: STREAK (Target = giorni consecutivi)
         */
        STREAK_3("Piccolo Passo", "Hai mantenuto uno streak di 3 giorni consecutivi!", "STREAK", "🌱", 3),
        STREAK_7("Settimana di Fuoco", "7 giorni consecutivi di gioco quotidiano!", "STREAK", "🔥", 7),
        STREAK_15("Costanza Incrollabile", "15 giorni di presenza ininterrotta!", "STREAK", "⚡", 15),
        STREAK_30("Mese Implacabile", "Un mese intero senza saltare un giorno!", "STREAK", "🌙", 30),
        STREAK_50("Mezzo Secolo", "50 giorni consecutivi di alveare!", "STREAK", "💥", 50),
        STREAK_100("Centenario", "100 giorni consecutivi di streak!", "STREAK", "💯", 100),
        STREAK_200("Inarrestabile", "200 giorni di presenza consecutiva!", "STREAK", "💎", 200),
        STREAK_365("Anno dell'Ape", "365 giorni consecutivi! Leggenda dell'alveare!", "STREAK", "🌟", 365),

        /*
         * CATEGORY: RANK TIER (Rank in single daily session, Target = ordine grado da 1
         * a 8)
         */
        RANK_FRESH_MIND("Mente Fresca", "Hai raggiunto il grado Mente Fresca!", "RANK", "🍃", 1),
        RANK_BEGINNER("Principiante", "Hai raggiunto il grado Principiante!", "RANK", "🐣", 2),
        RANK_ADVANCED("Avanzato", "Hai raggiunto il grado Avanzato!", "RANK", "🚀", 3),
        RANK_EXPERT("Esperto", "Hai raggiunto il grado Esperto!", "RANK", "💡", 4),
        RANK_EXCELLENT("Eccellente", "Hai raggiunto il grado Eccellente!", "RANK", "⭐", 5),
        RANK_GENIUS("Genio", "Hai raggiunto il grado Genio!", "RANK", "🧠", 6),
        RANK_MASTER("Maestro", "Hai raggiunto il grado Maestro!", "RANK", "👑", 7),
        RANK_REGINA("Ape Regina", "Hai raggiunto il grado massimo in una sessione di gioco!", "RANK", "🐝", 8),

        /*
         * CATEGORY: CAREER TIER (Seasonal / Annual career, Target = ordine tier da 1 a
         * 8)
         */
        CAREER_LARVA("Stadio Larva", "Sei diventato una Larva nella carriera stagionale!", "CAREER", "🐛", 1),
        CAREER_NURSE_BEE("Ape Nutrice", "Sei diventato un'Ape Nutrice nella carriera stagionale!", "CAREER", "🍼", 2),
        CAREER_WORKER_BEE("Ape Operaia", "Sei diventato un'Ape Operaia nella carriera stagionale!", "CAREER", "🛠️", 3),
        CAREER_FORAGER_BEE("Ape Bottinatrice", "Sei diventato un'Ape Bottinatrice nella carriera stagionale!", "CAREER",
                        "🌸", 4),
        CAREER_GUARDIAN_BEE("Ape Guardiana", "Sei diventato un'Ape Guardiana nella carriera stagionale!", "CAREER",
                        "🗝️", 5),
        CAREER_DEFENDER_BEE("Ape Difensore", "Sei diventato un'Ape Difensore nella carriera stagionale!", "CAREER",
                        "🛡️", 6),
        CAREER_ARCHITECT_BEE("Ape Architetto", "Sei diventato un'Ape Architetto nella carriera stagionale!", "CAREER",
                        "📐", 7),
        CAREER_SEASON_QUEEN("Regina della Stagione", "Hai raggiunto il livello massimo della carriera stagionale!",
                        "CAREER", "👑", 8),

        /*
         * CATEGORY: WORDS & GENERAL PROGRESSION (WORDS, Target = parole totali)
         */
        FIRST_WORD("Prima Parola", "Hai trovato la tua primissima parola in Beesagono!", "WORDS", "✍️", 1),
        WORDS_100("Centurione", "Hai trovato 100 parole totali!", "WORDS", "📝", 100),
        WORDS_500("Vocabolario Vivente", "Hai trovato 500 parole totali!", "WORDS", "📖", 500),
        WORDS_1000("Enciclopedia Umana", "Hai trovato 1.000 parole totali!", "WORDS", "🏛️", 1000),

        /*
         * CATEGORY: PANGRAMS / MIELEGRAMS (PANGRAM, Target = mielegrammi trovati)
         */
        FIRST_PANGRAM("Primo Mielegramma", "Hai trovato il tuo primo mielegramma!", "PANGRAM", "✨", 1),
        PANGRAM_10("Cacciatore di Mielegrammi", "Hai scovato 10 mielegrammi unici!", "PANGRAM", "🔍", 10),
        PANGRAM_50("Maestro del Mielegramma", "Hai scovato 50 mielegrammi unici!", "PANGRAM", "🍯", 50),

        /*
         * CATEGORY: PUZZLE COMPLETION (COMPLETION, Target = puzzle completati al 100%)
         */
        FIRST_COMPLETION("Perfezionista", "Hai completato al 100% il tuo primo puzzle!", "COMPLETION", "🎯", 1),
        COMPLETION_10("Collezionista dell'Alveare", "Hai completato al 100% 10 puzzle diversi!", "COMPLETION", "🏆",
                        10),

        /*
         * CATEGORY: DICTIONARY CONTRIBUTIONS (CONTRIBUTION, Target = suggerimenti
         * approvati)
         */
        FIRST_SUGGESTION_ACCEPTED("Lessicografo In erba",
                        "Una parola suggerita è stata approvata dagli Amministratori e aggiunta al dizionario!",
                        "CONTRIBUTION", "📌", 1),
        SUGGESTIONS_ACCEPTED_5("Linguista dell'Alveare",
                        "5 delle tue parole suggerite sono state approvate e aggiunte al dizionario!",
                        "CONTRIBUTION", "🤝", 5);

        private final String title;
        private final String description;
        private final String category;
        private final String iconUrl;
        private final int targetValue;

        BadgeCode(String title, String description, String category, String iconUrl, int targetValue) {
                this.title = title;
                this.description = description;
                this.category = category;
                this.iconUrl = iconUrl;
                this.targetValue = targetValue;
        }
}