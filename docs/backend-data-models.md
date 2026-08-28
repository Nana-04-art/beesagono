# Backend Data Model — Beesagono API

This document freezes the database schema, entity-to-frontend-model mapping, and the ported puzzle-generation algorithm.

---

## 1. Enums & Domain Constants

In the database, these are stored as portable `VARCHAR` columns with `CHECK` constraints (supported natively in MySQL 8+ and Oracle) or mapped via Java `@Enumerated(EnumType.STRING)`. In code, they are implemented as static domain Enums.

```sql
-- --------------------------------------------------------
-- DOMAIN ENUMS & CHECK CONSTRAINTS
-- --------------------------------------------------------

-- 1. Security Roles
-- role_name: 'ROLE_USER' | 'ROLE_ADMIN'

-- 2. Daily Rank Tiers (RankTier)
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

-- 3. Streak Milestones & Bonus Points (StreakMilestone)
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

-- 4. Career Tiers (CareerTier)
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

## 2. Core Tables (DBML)

```dbml
// --------------------------------------------------------
// DICTIONARY
// --------------------------------------------------------
Table dictionary_words {
  id                    varchar(36)  [pk]                 // UUID, app-generated
  word                  varchar(100) [not null, unique]   // always stored UPPERCASE
  length                int          [not null]
  unique_letters_count  int          [not null]
  is_candidate_pangram  boolean      [not null, default: false] // true if unique_letters_count = 7
  added_by_admin        boolean      [not null, default: false]
  created_at            timestamp    [not null]
}

// --------------------------------------------------------
// DAILY PUZZLE
// --------------------------------------------------------
Table daily_puzzles {
  id            varchar(36) [pk]                     // UUID, app-generated
  puzzle_date   date        [not null, unique]       // YYYY-MM-DD, local server date
  seed          varchar(50) [not null]               // "${baseSeed}_${attempt}", for debuggability
  center_letter char(1)     [not null]
  max_score     int         [not null]
  created_at    timestamp   [not null]
}

Table puzzle_outer_letters {
  puzzle_id varchar(36) [not null, ref: > daily_puzzles.id]
  letter    char(1)     [not null]

  indexes {
    (puzzle_id, letter) [pk]   // composite PK — prevents the same letter twice for one puzzle
  }
}

Table puzzle_possible_words {
  puzzle_id varchar(36)  [not null, ref: > daily_puzzles.id]
  word      varchar(100) [not null]

  indexes {
    (puzzle_id, word) [pk]
  }
}

Table puzzle_mielegrammi {
  puzzle_id varchar(36)  [not null, ref: > daily_puzzles.id]
  word      varchar(100) [not null]

  indexes {
    (puzzle_id, word) [pk]
  }
}

// --------------------------------------------------------
// USERS & AUTH
// --------------------------------------------------------
Table users {
  id            varchar(36)  [pk]
  username      varchar(50)  [not null, unique]
  email         varchar(150) [not null, unique]
  password_hash varchar(255) [not null]   // BCrypt hash, never plaintext
  created_at    timestamp    [not null]
}

Table roles {
  id   varchar(36) [pk]
  name varchar(20) [not null, unique]     // 'ROLE_USER' | 'ROLE_ADMIN'
}

Table user_roles {
  user_id varchar(36) [not null, ref: > users.id]
  role_id varchar(36) [not null, ref: > roles.id]

  indexes {
    (user_id, role_id) [pk]
  }
}

// --------------------------------------------------------
// REFRESH TOKENS (enables real logout / revocation)
// --------------------------------------------------------
Table refresh_tokens {
  id          varchar(36)  [pk]
  user_id     varchar(36)  [not null, ref: > users.id]
  token_hash  varchar(255) [not null, unique]   // SHA-256 hash of the opaque refresh token; raw value never stored
  expires_at  timestamp    [not null]
  revoked     boolean      [not null, default: false]
  created_at  timestamp    [not null]

  indexes {
    (user_id)
  }
}

// --------------------------------------------------------
// GAME STATE (per user, per day)
// --------------------------------------------------------
Table game_states {
  id             varchar(36) [pk]
  puzzle_id      varchar(36) [not null, ref: > daily_puzzles.id]
  user_id        varchar(36) [not null, ref: > users.id]
  current_score  int         [not null, default: 0]
  is_completed   boolean     [not null, default: false]
  start_time     timestamp   [not null]
  last_updated   timestamp   [not null]

  indexes {
    (user_id, puzzle_id) [unique]  // one game state per user per puzzle/day
  }
}

Table game_state_found_words {
  game_state_id varchar(36)  [not null, ref: > game_states.id]
  word          varchar(100) [not null]

  indexes {
    (game_state_id, word) [pk]
  }
}

Table game_state_invalid_words {
  game_state_id varchar(36)  [not null, ref: > game_states.id]
  word          varchar(100) [not null]

  indexes {
    (game_state_id, word) [pk]
  }
}

// --------------------------------------------------------
// PLAYER STATS (cross-day, one row per user)
// --------------------------------------------------------
Table player_stats {
  user_id           varchar(36) [pk, ref: - users.id]
  current_streak    int         [not null, default: 0]
  max_streak        int         [not null, default: 0]
  total_points      int         [not null, default: 0]  // = current season's totalSeasonPoints
  games_played      int         [not null, default: 0]
  games_completed   int         [not null, default: 0]
  last_played_date  date
}

// --------------------------------------------------------
// SEASON HISTORY (one row per user per calendar year)
// --------------------------------------------------------
Table player_seasons {
  user_id                 varchar(36) [not null, ref: > users.id]
  year                    int         [not null]
  base_points_earned      int         [not null, default: 0]
  bonus_streak_points     int         [not null, default: 0]
  total_season_points     int         [not null, default: 0]
  highest_tier_achieved   varchar(50) [not null]

  indexes {
    (user_id, year) [pk]
  }
}

Table player_season_milestones {
  user_id       varchar(36) [not null]
  year          int         [not null]
  streak_length int         [not null]   // one of the STREAK_MILESTONES keys (3, 7, 15, 30, 50, 100, 200, 365)

  indexes {
    (user_id, year, streak_length) [pk]
  }
}

// --------------------------------------------------------
// DAILY RANK DISTRIBUTION (histogram of ranks achieved per day)
// --------------------------------------------------------
Table player_daily_rank_distribution {
  user_id    varchar(36) [not null, ref: > users.id]
  rank_label varchar(50) [not null]  // e.g. "🐝 Ape Regina" — matches frontend RANK_TIERS.label verbatim
  count      int         [not null, default: 0]

  indexes {
    (user_id, rank_label) [pk]
  }
}
```

---

## 3. Ported Puzzle Generation Algorithm (Java)

`PuzzleGeneratorService.java` reproduces the frontend's `puzzle-generator.service.ts` **bit-for-bit**:

```java
// djb2-style date hash — matches hashDateString() in the frontend exactly
private long hashDateString(String date) {
    long hash = 5381L;
    for (int i = 0; i < date.length(); i++) {
        hash = ((hash * 33) ^ date.charAt(i)) & 0xFFFFFFFFL; // emulate JS `>>> 0`
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

> **Verification requirement:** before this port is trusted in production, it must reproduce the frontend's own frozen test vector exactly:
>
> | Input | Expected Value |
> | :--- | :--- |
> | `date` | `"2026-07-27"` |
> | `hashDateString` | `1385072001` |
> | resulting letters | `C, E, G, N, O, R, S` |
> | center letter | `R` |
> | mielegrammi | `["CONGRESSO"]` |
>
> This should be encoded as a JUnit test (`PuzzleGeneratorServiceTest`) mirroring the frontend's own `puzzle-generator.service.spec.ts`, run against the exact same `dictionary_words` snapshot the frontend's `dictionary.json` was seeded from.

### Generation flow (server-side)
1. On `GET /api/v1/puzzle/today`, look up `daily_puzzles` by `puzzle_date = today`.
2. If found, return it (and its child rows) directly — **never regenerate an existing date**.
3. If not found:
   a. `SELECT word FROM dictionary_words WHERE is_candidate_pangram = true` → candidate pool (fallback to the literal `"ALBERGO"` if empty, matching the frontend's safety net).
   b. Run the PRNG loop exactly as in `data-models.md §8` of the frontend docs (pick a candidate, derive 7 sorted unique letters, pick a center letter, compute `possibleWords`/`mielegrammi` against the full `dictionary_words` table, check the Quality Gate: ≥15 words, ≥1 mielegramma), up to `MAX_GENERATION_ATTEMPTS` (500).
   c. Persist the accepted (or last-attempt fallback) board into `daily_puzzles` + `puzzle_outer_letters` + `puzzle_possible_words` + `puzzle_mielegrammi` in a single transaction.
   d. Return the newly created puzzle.

---

## 4. Rank & Career Tier Configuration

`RANK_TIERS`, `CAREER_TIERS`, and `STREAK_MILESTONES` are **not** modeled as database tables — they are small, rarely-changing configuration sets, kept as static Java classes (`RankTiers.java`, `CareerTiers.java`) that mirror the frontend's `rank-tiers.config.ts` / `career-tiers.constant.ts` **verbatim**, including the emoji-prefixed labels, so a `rankLabel` string computed on the backend is byte-for-byte identical to one a legacy guest-mode frontend session would have computed:

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

Career tiers (`highest_tier_achieved`, `player_seasons.highest_tier_achieved`):

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

If a future requirement needs admin-editable ranks/tiers, these tables would be promoted to real DB tables (`rank_tiers`, `career_tiers`, `streak_milestones`) at that point — out of scope for the current spec.

---

## 5. Frontend Model ↔ Backend Schema Mapping

| Frontend concept | Backend representation | Notes |
| :--- | :--- | :--- |
| `GameBoard.cells` (center + 6 outer) | `daily_puzzles.center_letter` + `puzzle_outer_letters` | Position/order is not persisted (client-only concern, see §1). |
| `GameBoard.possibleWords` | `puzzle_possible_words` | |
| `GameBoard.mielegrammi` | `puzzle_mielegrammi` | |
| `GameBoard.maxScore` | `daily_puzzles.max_score` | |
| `GameState.score` | `game_states.current_score` | Renamed `score` → `current_score` in the DB/API to avoid ambiguity with `player_stats.total_points`. |
| `GameState.foundWords` | `game_state_found_words` | |
| `GameState.invalidWords` | `game_state_invalid_words` | |
| `GameState.foundMielegrammi` | *(not persisted — derived)* | Same as the frontend: computed as `foundWords ∩ puzzle_mielegrammi`, never stored as its own column/table. |
| `GameState.isCompleted` | `game_states.is_completed` | |
| `GameState.rankLabel` | *(not persisted — derived)* | Recomputed on every read from `current_score / max_score`, exactly like the frontend never trusts a stored rank. |
| `PlayerStats` (all fields) | `player_stats` | |
| `PlayerStats.currentSeason` | `player_seasons` (row where `year` = current year) | |
| `PlayerStats.currentSeason.claimedStreakMilestones` | `player_season_milestones` | |
| `PlayerStats.seasonHistory` | `player_seasons` (all rows where `year` < current year) | Same table serves both current and historical seasons — "current" is just "the row for this year." |
| `PlayerStats.dailyRankDistribution` | `player_daily_rank_distribution` | |

---

## 6. Portability Notes (MySQL / Oracle)

- `boolean` → MySQL native `BOOLEAN` (alias for `TINYINT(1)`); Oracle has no native boolean prior to 23c, so map to `NUMBER(1)` via Hibernate's boolean-to-numeric converter — handled transparently by JPA's `@Convert` or the platform dialect, no schema-level branching needed in application code.
- `varchar(36)` UUIDs avoid any dependency on auto-increment/sequence semantics, which differ meaningfully between MySQL (`AUTO_INCREMENT`) and Oracle (`SEQUENCE` + trigger, or `GENERATED AS IDENTITY` from 12c+).
- `timestamp` columns should be stored in UTC at the application layer regardless of engine, to avoid session-timezone drift between MySQL and Oracle connections.