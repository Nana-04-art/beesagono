# Backend Data Model — Beesagono API

This document freezes the database schema (**MySQL 8.0+ / InnoDB**), entity-to-frontend-model mapping, and the ported puzzle-generation algorithm.

---

## 1. Enums & Domain Constants

Enforced via lookup tables or application-level Enums.

```sql
-- --------------------------------------------------------
-- DOMAIN ENUMS & LOOKUPS
-- --------------------------------------------------------

-- 1. Security Roles
-- role_name: 'ROLE_USER' | 'ROLE_ADMIN'

-- 2. Error Types Lookup (error_types.code)
-- 'TOO_SHORT' | 'MISSING_CENTER' | 'INVALID_LETTERS' | 'ALREADY_FOUND' | 'NOT_IN_DICTIONARY'

-- 3. Daily Rank Tiers (RankTier)
-- threshold_percent | label             | icon
-- ----------------------------------------------
-- 0.0               | 'Iniziato'        | '🌱'
-- 2.0               | 'Mente Fresca'    | '🍃'
-- 5.0               | 'Principiante'    | '🐣'
-- 8.0               | 'Avanzato'        | '🚀'
-- 15.0              | 'Esperto'         | '💡'
-- 25.0              | 'Eccellente'      | '⭐'
-- 40.0              | 'Genio'           | '🧠'
-- 70.0              | 'Maestro'         | '👑'
-- 100.0             | 'Ape Regina'      | '🐝'

-- 4. Streak Milestones & Bonus Points (StreakMilestone)
-- streak_days | bonus_points
-- --------------------------
-- 3           | 50
-- 7           | 150
-- 15          | 350
-- 30          | 800
-- 50          | 1500
-- 100         | 3500
-- 200         | 8000
-- 365         | 20000

-- 5. Career Tiers (CareerTier)
-- min_percentage | title                        | icon
-- --------------------------------------------------
-- 0              | 'Uovo d'Ape'                 | '🥚'
-- 15             | 'Larva'                      | '🐛'
-- 30             | 'Ape Nutrice'                | '🍼'
-- 45             | 'Ape Operaia'                | '🐝'
-- 60             | 'Ape Bottinatrice'           | '🌸'
-- 75             | 'Ape Custode'                | '🛡️'
-- 85             | 'Ape Guardiana'              | '⚔️'
-- 95             | 'Ape Architetto'             | '📐'
-- 100            | 'Ape Regina della Stagione'  | '👑'

```

---

## 2. Core Tables (DBML)

```dbml
Project beesagono_db {
  database_type: 'MySQL 8.0+'
  note: '''
    ENGINE: InnoDB for all tables (required for foreign keys and row-level
    locking under concurrent word submissions).

    CHARSET: the whole database, and every table/column that can contain an
    emoji (dictionary/rank labels, error descriptions), MUST use
    utf8mb4 / utf8mb4_0900_ai_ci (or utf8mb4_unicode_ci on older MySQL).
    Plain "utf8" in MySQL is actually utf8mb3 and cannot store 4-byte
    characters such as emoji — inserts will either fail or silently
    truncate/corrupt the rank label. Set this at the database level
    (CREATE DATABASE ... CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci)
    so every table inherits it by default.

    All surrogate primary keys are VARCHAR(36) UUID strings, generated
    application-side (UUID.randomUUID().toString()).
    All timestamps are stored in UTC.
  '''
}

// ==========================================
// ENUMS & LOOKUPS
// ==========================================

Enum role_name {
  ROLE_USER
  ROLE_ADMIN
}

Table error_types {
  code varchar(20) [pk, note: 'TOO_SHORT | MISSING_CENTER | INVALID_LETTERS | ALREADY_FOUND | NOT_IN_DICTIONARY']
  description varchar(255) [not null]
}

Table roles {
  id varchar(36) [pk]
  name role_name [not null, unique]
}

// ==========================================
// USERS & AUTHENTICATION
// ==========================================

Table users {
  id varchar(36) [pk]
  username varchar(50) [not null, unique]
  email varchar(150) [not null, unique]
  password_hash varchar(255) [not null]
  registered_at timestamp [not null]
}

Table user_roles {
  user_id varchar(36) [not null]
  role_id varchar(36) [not null]

  Indexes {
    (user_id, role_id) [pk]
  }
}

Table refresh_tokens {
  id varchar(36) [pk]
  user_id varchar(36) [not null]
  token_hash varchar(255) [not null, unique]
  expires_at timestamp [not null]
  revoked boolean [not null, default: false]
  created_at timestamp [not null]

  Indexes {
    user_id
  }
}

// ==========================================
// DICTIONARY
// ==========================================

Table dictionary_words {
  word varchar(100) [pk, note: 'Always stored UPPERCASE']
  word_length int [not null, note: 'Could be a MySQL virtual/generated column (CHAR_LENGTH(word)) instead of an app-computed one — optional optimization']
  unique_letters_count int [not null]
  is_candidate_pangram boolean [not null, default: false]
  added_by_user_id varchar(36) [note: 'NULL = seeded at launch / not attributable to a specific admin. Replaces the old added_by_admin boolean for real traceability.']
  added_at timestamp [not null]

  Indexes {
    is_candidate_pangram
  }
}

// ==========================================
// DAILY PUZZLE
// ==========================================

Table daily_puzzles {
  id varchar(36) [pk]
  puzzle_date date [not null, unique]
  center_letter char(1) [not null]
  max_score int [not null]
  seed varchar(50)
  created_at timestamp [not null]
}

Table puzzle_outer_letters {
  puzzle_id varchar(36) [not null]
  letter char(1) [not null]

  Indexes {
    (puzzle_id, letter) [pk]
  }
}

Table puzzle_words {
  puzzle_id varchar(36) [not null]
  word varchar(100) [not null]
  is_mielegramma boolean [not null, default: false]

  Indexes {
    (puzzle_id, word) [pk]
  }
}

// ==========================================
// GAME SESSIONS
// ==========================================

Table game_sessions {
  id varchar(36) [pk]
  puzzle_id varchar(36) [not null]
  user_id varchar(36) [not null]
  current_score int [not null, default: 0]
  current_rank_label varchar(50) [not null, note: 'Denormalized cache of score/max_score resolved against RANK_TIERS — must be written in the same transaction as current_score, never independently']
  is_completed boolean [not null, default: false]
  start_time timestamp [not null]
  last_updated timestamp [not null]

  Indexes {
    (user_id, puzzle_id) [unique]
    puzzle_id [note: 'Added for daily-leaderboard queries ("all sessions for today\'s puzzle") — the unique index above has user_id as its leading column, so it does not efficiently serve a puzzle_id-only lookup']
  }
}

Table found_words {
  session_id varchar(36) [not null]
  word varchar(100) [not null]
  found_at timestamp [not null]

  Indexes {
    (session_id, word) [pk]
  }
}

Table invalid_word_attempts {
  id varchar(36) [pk]
  session_id varchar(36) [not null]
  attempted_word varchar(100) [not null]
  error_reason varchar(20) [not null]
  attempted_at timestamp [not null]

  Indexes {
    session_id
  }
}

// ==========================================
// PLAYER STATISTICS & SEASONS
// ==========================================

Table player_stats {
  user_id varchar(36) [pk]
  current_streak int [not null, default: 0]
  max_streak int [not null, default: 0]
  total_points int [not null, default: 0, note: 'Denormalized cache of the CURRENT season row in player_seasons.total_points — must be updated in the same transaction whenever that row changes']
  games_played int [not null, default: 0]
  games_completed int [not null, default: 0]
  last_played_date date
}

Table player_seasons {
  user_id varchar(36) [not null]
  year int [not null]
  base_points int [not null, default: 0]
  bonus_points int [not null, default: 0]
  total_points int [not null, default: 0, note: 'Denormalized base_points + bonus_points — see player_stats.total_points note above for the second sync obligation this creates']
  highest_tier_achieved varchar(50)

  Indexes {
    (user_id, year) [pk]
  }
}

Table milestone_redemptions {
  user_id varchar(36) [not null]
  year int [not null]
  streak_length int [not null]
  redeemed_at timestamp [not null]

  Indexes {
    (user_id, year, streak_length) [pk]
  }
}

Table rank_histogram {
  user_id varchar(36) [not null]
  rank_label varchar(50) [not null]
  count int [not null, default: 0]

  Indexes {
    (user_id, rank_label) [pk]
  }
}

// ==========================================
// EXPLICIT FOREIGN KEYS (with ON DELETE / ON UPDATE actions)
// ==========================================

// Authentication & Roles
Ref: user_roles.user_id > users.id [delete: cascade, update: cascade]
Ref: user_roles.role_id > roles.id [delete: restrict, update: cascade]
Ref: refresh_tokens.user_id > users.id [delete: cascade, update: cascade]

// Dictionary attribution
Ref: dictionary_words.added_by_user_id > users.id [delete: set null, update: cascade]

// Puzzle & Dictionary
Ref: puzzle_outer_letters.puzzle_id > daily_puzzles.id [delete: cascade, update: cascade]
Ref: puzzle_words.puzzle_id > daily_puzzles.id [delete: cascade, update: cascade]
Ref: puzzle_words.word > dictionary_words.word [delete: restrict, update: cascade]

// Game Sessions & Logs
Ref: game_sessions.user_id > users.id [delete: cascade, update: cascade]
Ref: game_sessions.puzzle_id > daily_puzzles.id [delete: restrict, update: cascade]
Ref: found_words.session_id > game_sessions.id [delete: cascade, update: cascade]
Ref: invalid_word_attempts.session_id > game_sessions.id [delete: cascade, update: cascade]
Ref: invalid_word_attempts.error_reason > error_types.code [delete: restrict, update: cascade]

// Player Stats & Seasons
Ref: player_stats.user_id - users.id [delete: cascade, update: cascade]
Ref: player_seasons.user_id > users.id [delete: cascade, update: cascade]
Ref: rank_histogram.user_id > users.id [delete: cascade, update: cascade]
Ref: milestone_redemptions.(user_id, year) > player_seasons.(user_id, year) [delete: cascade, update: cascade]

```

---

## 3. Ported Puzzle Generation Algorithm (Java)

`PuzzleGeneratorService.java` reproduces the frontend's generator **bit-for-bit**:

```java
private long hashDateString(String date) {
    long hash = 5381L;
    for (int i = 0; i < date.length(); i++) {
        hash = ((hash * 33) ^ date.charAt(i)) & 0xFFFFFFFFL;
    }
    return hash;
}

// Mulberry32 PRNG — matches mulberry32() in the frontend exactly
private long state;

private void seed(long seed) {
    this.state = seed & 0xFFFFFFFFL;
}

private double nextRandom() {
    state = (state + 0x6D2B79F5L) & 0xFFFFFFFFL;
    long t = state;
    t = (t ^ (t >>> 15)) * ((t | 1L) & 0xFFFFFFFFL);
    t &= 0xFFFFFFFFL;
    t = (t + (((t ^ (t >>> 7)) * ((t | 61L) & 0xFFFFFFFFL)) & 0xFFFFFFFFL)) & 0xFFFFFFFFL;
    return ((t ^ (t >>> 14)) & 0xFFFFFFFFL) / 4294967296.0;
}

```

> **Test Vector Check:** `date = "2026-07-27"` → `hash = 1385072001` → Letters: `C, E, G, N, O, R, S` (Center: `R`).
> 
> 

---

## 4. Rank & Career Tier Configuration

`RANK_TIERS`, `CAREER_TIERS`, and `STREAK_MILESTONES` are kept as static Java classes (`RankTiers.java`, `CareerTiers.java`) mirroring the frontend's configuration **verbatim**:

| Threshold (%) | Rank Label |
| :--- | :--- |
| 0 | 🌱 Iniziato |
| 2 | 🍃 Mente Fresca |
| 5 | 🐣 Principiante |
| 8 | 🚀 Avanzato |
| 15 | 💡 Esperto |
| 25 | ⭐ Eccellente |
| 40 | 🧠 Genio |
| 70 | 👑 Maestro |
| 100 | 🐝 Ape Regina |

| Streak length (days) | Bonus points |
| :--- | :--- |
| 3 | 50 |
| 7 | 150 |
| 15 | 350 |
| 30 | 800 |
| 50 | 1500 |
| 100 | 3500 |
| 200 | 8000 |
| 365 | 20000 |

Career tiers (`highest_tier_achieved`):

| minPercentage (%) | Career Tier |
| :--- | :--- |
| 0 | Uovo d'Ape |
| 15 | Larva |
| 30 | Ape Nutrice |
| 45 | Ape Operaia |
| 60 | Ape Bottinatrice |
| 75 | Ape Custode |
| 85 | Ape Guardiana |
| 95 | Ape Architetto |
| 100 | Ape Regina della Stagione |

---

## 5. Frontend Model ↔ Backend Schema Mapping

| Frontend concept | Backend representation (MySQL) | Notes |
| --- | --- | --- |
| `GameBoard.cells` | `daily_puzzles.center_letter` + `puzzle_outer_letters` | Order is client-only. |
| `GameBoard.possibleWords` | `puzzle_words` | Filtered where `is_mielegramma = false`. |
| `GameBoard.mielegrammi` | `puzzle_words` | Filtered where `is_mielegramma = true`. |
| `GameState.score` | `game_sessions.current_score` | Updated atomically. |
| `GameState.foundWords` | `found_words` | Append-only per session. |
| `GameState.invalidWords` | `invalid_word_attempts` | Full audit log. |
| `PlayerStats` | `player_stats` | Primary key: `user_id`. |
| `PlayerStats.currentSeason` | `player_seasons` | Filtered by current `year`. |

---

## 5. MySQL Best Practices & Optimization Notes

* **Engine:** Strict `InnoDB` on all tables for ACID compliance and row-level locking during word submissions.


* **Charset/Collation:** Database-wide `utf8mb4` / `utf8mb4_0900_ai_ci` to natively support emojis in rank titles and dictionary labels.


* **Keys:** Application-generated `VARCHAR(36)` UUID strings.


* **Timezones:** All `timestamp` columns strictly store UTC.