# Backend Architecture — Beesagono API

This document defines the server-side architecture that turns Beesagono from a fully client-side PWA into a **client-server application** with account persistence, cross-device sync, and a tamper-resistant scoring/stats authority. It complements `backend-data-models.md`, `api-contracts.md`, and `sync-strategy.md`.

---

## 1. Motivation & Scope

The original frontend-only architecture (see the Angular project's own `frontend-architecture.md`) kept the dictionary, puzzle generation, scoring, and statistics entirely in the browser (`localStorage`). This has two structural weaknesses once accounts are introduced:

1. **No cross-device continuity** — progress is trapped in a single browser.
2. **No anti-cheat guarantee** — `puzzle_words`, `mielegrammi`, and score math are all inspectable/editable client-side.

The backend addresses both by becoming the **single source of truth** for: the daily puzzle, dictionary, word validation, scoring, and player statistics. The Angular frontend is simplified into a thin client that renders whatever the API returns and no longer ships `dictionary.json` or runs `PuzzleGeneratorService`/`DictionaryService` locally.

Guest (no-account) play is still supported for zero-friction onboarding — see `sync-strategy.md` for how guest progress merges into an account at registration/login time.

---

## 2. Technology Stack

| Layer | Technology |
| :--- | :--- |
| **Language / Runtime** | Java 21 |
| **Framework** | Spring Boot 4.0.3 |
| **Web Layer** | Spring Web (REST controllers, `@RestControllerAdvice` for error handling) |
| **Persistence** | Spring Data JPA + Hibernate (MySQL Dialect: `org.hibernate.dialect.MySQLDialect`) |
| **Database** | MySQL 8.0+ (InnoDB engine, `utf8mb4` charset) — see `backend-data-model.md` |
| **Security** | Spring Security 6, stateless JWT (access + refresh tokens), BCrypt password hashing |
| **Validation** | Jakarta Bean Validation (`jakarta.validation`) on request DTOs |
| **Build** | Maven or Gradle (examples in this doc set assume Maven) |
| **API Style** | REST, JSON, versioned under `/api/v1` |

> **ID strategy:** every entity's primary key is a `VARCHAR(36)` UUID string, generated **application-side** (`UUID.randomUUID().toString()`) rather than DB auto-increment. This keeps IDs non-guessable, avoids auto-increment locking under high concurrency, and lets the application assign an ID before the first `INSERT` (useful for puzzle generation and guest-merge flows).

---

## 3. Layered Architecture

```text
+----------------------------------------------------------------------+
|                         CLIENT (Angular)                             |
|   Thin renderer: fetches /puzzle/today, submits words, renders UI    |
+------------------------------------+---------------------------------+
                                     | HTTPS / JSON / JWT Bearer
                                     v
+----------------------------------------------------------------------+
|                        CONTROLLER LAYER                              |
|  PuzzleController | AuthController | GameController | StatsController|
|  AdminDictionaryController                                           |
|  - Request/DTO validation (Jakarta Validation)                       |
|  - Delegates to Service layer, never touches repositories directly   |
+----------------------------------+-----------------------------------+
                                   v
+----------------------------------+-----------------------------------+
|                          SERVICE LAYER                               |
|  PuzzleGeneratorService (Java port of the Mulberry32/djb2 algorithm) |
|  DictionaryService      (DB-backed word lookups, admin word CRUD)    |
|  ScoreService           (word points, rank resolution — mirrors the  |
|                           frontend ScoreService 1:1)                 |
|  GameStateService       (validate + persist word submissions)        |
|  StatsService           (streaks, seasons, career tiers — mirrors    |
|                           the frontend StatsService 1:1)             |
|  AuthService            (register/login/refresh/logout, JWT issuance)|
|  SyncService            (guest -> account merge, device merge)       |
+-----------------------------------+----------------------------------+
                                    v
+-----------------------------------+----------------------------------+
|                        REPOSITORY LAYER                              |
|         Spring Data JPA Repositories (one per Entity/Aggregate)      |
+-----------------------------------+----------------------------------+
                                    v
+-----------------------------------+-----------------------------------+
|                          DATABASE (MySQL)                             |
|              See backend-data-model.md for full schema                |
+-----------------------------------------------------------------------+

```

### Design rules

* **Controllers are thin.** They validate the request shape (via `@Valid` DTOs) and delegate to exactly one service method; no business logic lives in a controller.
* **Services own transactions.** Any method that writes to more than one table (e.g. `GameStateService.submitWord()`, which updates `game_sessions`, `found_words`, and triggers `StatsService`) is annotated `@Transactional` so the write is atomic.
* **Entities are never returned directly from controllers.** Every response is a DTO, mapped from the entity in the service layer, so the wire format is decoupled from the JPA schema and can evolve independently.
* **The frontend's business logic is the specification.** Wherever the Angular codebase already encodes a rule (word length, mielegramma bonus, rank thresholds, streak milestones), the Java service reproduces it **exactly** rather than re-deriving it, to guarantee score/rank parity between what the guest-mode frontend used to compute and what the authenticated backend now computes.

---

## 4. Puzzle Generation — Becomes a Backend Responsibility

* All PRNG logic (Mulberry32) and the `hashDateString` (djb2 variant) hashing are ported **bit-for-bit** into `PuzzleGeneratorService.java`, using Java's 32-bit `int` arithmetic and explicit unsigned-shift equivalents (`>>> 0` in TypeScript maps to using `int` with `>>>` unsigned right shift in Java, since Java `int` is natively 32-bit signed).
* **Generation trigger:** puzzles are generated **lazily**, on the first `GET /api/v1/puzzle/today` request for a date that has no row yet in `daily_puzzles`. A scheduled job (`@Scheduled` cron, e.g. run at `00:01` UTC) may optionally pre-generate the next day's puzzle to avoid a cold-start on the first request of the day, but lazy generation remains the fallback source of truth.
* Once generated, `daily_puzzles` (+ its child tables `puzzle_outer_letters` and `puzzle_words`) is **immutable** for that date — regenerating it would break score determinism for anyone who already played.
* **Dictionary source:** `dictionary_words` replaces the bundled `dictionary.json`; `is_candidate_pangram` is a precomputed flag (word has exactly 7 unique letters) refreshed whenever a word is added, so the generator's "extract pangram candidates" step becomes a single indexed `WHERE is_candidate_pangram = true` query instead of an in-memory filter over the whole dictionary.

---

## 5. Security Architecture

* **Password storage:** BCrypt (`BCryptPasswordEncoder`, default strength 10+), never plaintext or reversibly encrypted.
* **Authentication:** stateless JWT.
* **Access token:** short-lived (recommended default: **15 minutes**), included as `Authorization: Bearer <token>`, carries `userId`, `username`, and `roles` as claims.
* **Refresh token:** long-lived (recommended default: **30 days**), opaque random string, persisted server-side (hashed) in the `refresh_tokens` table (see `backend-data-model.md`) so it can be **revoked** — this enables real logout/invalidation, since a bare JWT access token cannot be invalidated before its natural expiry without server-side state.
* `POST /api/v1/auth/refresh` exchanges a valid, non-revoked refresh token for a new access token (and optionally rotates the refresh token).
* `POST /api/v1/auth/logout` revokes the refresh token (marks it `revoked = true`), so it can no longer be exchanged.


* **Authorization:** method-level (`@PreAuthorize("hasRole('ADMIN')")`) for admin-only endpoints (`/api/v1/admin/**`), backed by the `roles` claim embedded in the access token and re-validated against the `user_roles` table on sensitive operations.
* **Transport:** HTTPS is mandatory in all environments except local development; JWTs are never accepted over plaintext HTTP in production.
* **Guest endpoints** (`/api/v1/puzzle/today`, `/api/v1/puzzle/validate-word`) require no authentication and never touch user-scoped tables.

---

## 6. Error Handling Strategy

A single `@RestControllerAdvice` (`GlobalExceptionHandler`) maps exceptions to a consistent JSON error envelope (RFC 7807 `ProblemDetail`-style) across all controllers — see `api-contracts.md §0` for the exact shape and status-code mapping. This avoids each controller hand-rolling its own error responses and keeps client-side error handling uniform.

---

## 7. Configuration & Environment

* **Database Configuration:** DataSource settings are externalized via Spring properties (`application.yml` / `application-prod.yml`), using standard `org.hibernate.dialect.MySQLDialect`.
* **CORS:** configured to allow the Angular frontend's origin(s); credentials are not needed since auth uses `Authorization` headers rather than cookies.
* **Secrets:** JWT signing key, DB credentials, and master keys are externalized via environment variables or a secrets manager — never committed to source control.