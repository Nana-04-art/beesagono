package com.beesagono.backend.enums;

import lombok.Getter;

@Getter
public enum BadgeCode {

    /*
     * CATEGORY: STREAK (Mapped from GameConstants.STREAK_MILESTONES)
     */
    STREAK_3("Piccolo Passo", "Hai mantenuto uno streak di 3 giorni consecutivi!", "STREAK", "🌱"),
    STREAK_7("Settimana di Fuoco", "7 giorni consecutivi di gioco quotidiano!", "STREAK", "🔥"),
    STREAK_15("Costanza Incrollabile", "15 giorni di presenza ininterrotta!", "STREAK", "⚡"),
    STREAK_30("Mese Implacabile", "Un mese intero senza saltare un giorno!", "STREAK", "🌙"),
    STREAK_50("Mezzo Secolo", "50 giorni consecutivi di alveare!", "STREAK", "💥"),
    STREAK_100("Centenario", "100 giorni consecutivi di streak!", "STREAK", "💯"),
    STREAK_200("Inarrestabile", "200 giorni di presenza consecutiva!", "STREAK", "💎"),
    STREAK_365("Anno dell'Ape", "365 giorni consecutivi! Leggenda dell'alveare!", "STREAK", "🌟"),

    /*
     * CATEGORY: RANK TIER (Rank in single daily session)
     */
    RANK_FRESH_MIND("Mente Fresca", "Hai raggiunto il grado Mente Fresca!", "RANK", "🍃"),
    RANK_BEGINNER("Principiante", "Hai raggiunto il grado Principiante!", "RANK", "🐣"),
    RANK_ADVANCED("Avanzato", "Hai raggiunto il grado Avanzato!", "RANK", "🚀"),
    RANK_EXPERT("Esperto", "Hai raggiunto il grado Esperto!", "RANK", "💡"),
    RANK_EXCELLENT("Eccellente", "Hai raggiunto il grado Eccellente!", "RANK", "⭐"),
    RANK_GENIUS("Genio", "Hai raggiunto il grado Genio!", "RANK", "🧠"),
    RANK_MASTER("Maestro", "Hai raggiunto il grado Maestro!", "RANK", "👑"),
    RANK_REGINA("Ape Regina", "Hai raggiunto il grado massimo in una sessione di gioco!", "RANK", "🐝"),

    /*
     * CATEGORY: CAREER TIER (Seasonal / Annual career)
     */
    CAREER_LARVA("Stadio Larva", "Sei diventato una Larva nella carriera stagionale!", "CAREER", "🐛"),
    CAREER_NURSE_BEE("Ape Nutrice", "Sei diventato un'Ape Nutrice nella carriera stagionale!", "CAREER", "🍼"),
    CAREER_WORKER_BEE("Ape Operaia", "Sei diventato un'Ape Operaia nella carriera stagionale!", "CAREER", "🛠️"),
    CAREER_FORAGER_BEE("Ape Bottinatrice", "Sei diventato un'Ape Bottinatrice nella carriera stagionale!", "CAREER",
            "🌸"),
    CAREER_GUARDIAN_BEE("Ape Guardiana", "Sei diventato un'Ape Guardiana nella carriera stagionale!", "CAREER", "🗝️"),
    CAREER_DEFENDER_BEE("Ape Difensore", "Sei diventato un'Ape Difensore nella carriera stagionale!", "CAREER", "🛡️"),
    CAREER_ARCHITECT_BEE("Ape Architetto", "Sei diventato un'Ape Architetto nella carriera stagionale!", "CAREER",
            "📐"),
    CAREER_SEASON_QUEEN("Regina della Stagione", "Hai raggiunto il livello massimo della carriera stagionale!",
            "CAREER", "👑"),

    /*
     * CATEGORY: WORDS & GENERAL PROGRESSION (WORDS) Writing and reading progression
     */
    FIRST_WORD("Prima Parola", "Hai trovato la tua primissima parola in Beesagono!", "WORDS", "✍️"),
    WORDS_100("Centurione", "Hai trovato 100 parole totali!", "WORDS", "📝"),
    WORDS_500("Vocabolario Vivente", "Hai trovato 500 parole totali!", "WORDS", "📖"),
    WORDS_1000("Enciclopedia Umana", "Hai trovato 1.000 parole totali!", "WORDS", "🏛️"),

    /*
     * CATEGORY: PANGRAMS / MIELEGRAMS (PANGRAM) Rare pangram mastery icons
     */
    FIRST_PANGRAM("Primo Mielegramma", "Hai trovato il tuo primo mielegramma!", "PANGRAM", "✨"),
    PANGRAM_10("Cacciatore di Mielegrammi", "Hai scovato 10 mielegrammi unici!", "PANGRAM", "🔍"),
    PANGRAM_50("Maestro del Mielegramma", "Hai scovato 50 mielegrammi unici!", "PANGRAM", "🍯"),

    /*
     * CATEGORY: PUZZLE COMPLETION (COMPLETION) Perfection milestones
     */
    FIRST_COMPLETION("Perfezionista", "Hai completato al 100% il tuo primo puzzle!", "COMPLETION", "🎯"),
    COMPLETION_10("Collezionista dell'Alveare", "Hai completato al 100% 10 puzzle diversi!", "COMPLETION", "🏆"),

    /*
     * CATEGORY: DICTIONARY CONTRIBUTIONS (SUGGESTIONS / ADMIN)
     */
    FIRST_SUGGESTION_ACCEPTED("Lessicografo In erba",
            "Una parola suggerita è stata approvata dagli Amministratori e aggiunta al dizionario!",
            "CONTRIBUTION", "📌"),
    SUGGESTIONS_ACCEPTED_5("Linguista dell'Alveare",
            "5 delle tue parole suggerite sono state approvate e aggiunte al dizionario!",
            "CONTRIBUTION", "🤝");

    private final String title;
    private final String description;
    private final String category;
    private final String iconUrl;

    BadgeCode(String title, String description, String category, String iconUrl) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.iconUrl = iconUrl;
    }
}