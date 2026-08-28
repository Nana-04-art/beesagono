# API Contracts — Beesagono Backend

REST API contract for the Beesagono backend. All endpoints are versioned under `/api/v1`. All entity identifiers are **UUID strings** (`varchar(36)`), matching the database schema in `backend-data-model.md`.

---

## 0. Conventions

### 0.1 Authentication
Protected endpoints require:
```
Authorization: Bearer <ACCESS_JWT_TOKEN>
```

### 0.2 Standard Error Envelope
All error responses (4xx/5xx) share a single shape, in the style of RFC 7807 `ProblemDetail`:

```json
{
  "timestamp": "2026-08-28T14:32:10Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "La password deve contenere almeno 8 caratteri.",
  "path": "/api/v1/auth/register"
}
```

| HTTP Status | `error` code | Typical cause |
| :--- | :--- | :--- |
| 400 | `VALIDATION_ERROR` | Malformed/missing request fields (Jakarta Validation failure) |
| 401 | `UNAUTHORIZED` | Missing, invalid, or expired access token |
| 403 | `FORBIDDEN` | Valid token but insufficient role (e.g. non-admin calling `/admin/**`) |
| 404 | `NOT_FOUND` | Resource does not exist (e.g. no `GameState` for the given date) |
| 409 | `CONFLICT` | Unique-constraint violation (e.g. `username`/`email` already registered, duplicate word) |
| 422 | `UNPROCESSABLE_ENTITY` | Well-formed but semantically invalid input (e.g. `date` is not a valid ISO date) |
| 500 | `INTERNAL_ERROR` | Unexpected server fault |

### 0.3 Dates
All dates use ISO format `YYYY-MM-DD`, evaluated against the **server's local date**, matching the frontend's own local-date policy for daily rollover.

---

## 1. Public Endpoints (Guest / Game Engine)

No authentication required. These endpoints never read or write user-scoped tables.

### `GET /api/v1/puzzle/today`
Retrieves today's puzzle. If not yet persisted, the server generates it deterministically (PRNG, see `backend-data-model.md §4`), saves it, and returns it.

- **Headers:** none
- **Response `200 OK`:**
```json
{
  "date": "2026-08-28",
  "seed": "5381_0",
  "cells": [
    { "letter": "I", "isCenter": true,  "position": 0 },
    { "letter": "P", "isCenter": false, "position": 1 },
    { "letter": "O", "isCenter": false, "position": 2 },
    { "letter": "E", "isCenter": false, "position": 3 },
    { "letter": "T", "isCenter": false, "position": 4 },
    { "letter": "A", "isCenter": false, "position": 5 },
    { "letter": "R", "isCenter": false, "position": 6 }
  ],
  "maxScore": 342,
  "totalPossibleWords": 45,
  "totalMielegrammi": 2
}
```
> Note: `position` in the response is assigned arbitrarily by the server at serialization time (0 = center, 1–6 = outer in DB row order) purely for frontend rendering convenience — it carries no persisted meaning server-side (see `backend-data-model.md §1`).

### `POST /api/v1/puzzle/validate-word`
Validates a word for a guest user, **without persisting any state**.

- **Request Body:**
```json
{ "date": "2026-08-28", "word": "RIPORTARE" }
```
- **Response `200 OK` (valid word):**
```json
{
  "word": "RIPORTARE",
  "isValid": true,
  "pointsAwarded": 16,
  "isMielegramma": true,
  "errorType": null,
  "message": "Mielegramma trovato!"
}
```
- **Response `200 OK` (invalid word):**
```json
{
  "word": "CASA",
  "isValid": false,
  "pointsAwarded": 0,
  "isMielegramma": false,
  "errorType": "INVALID_LETTERS",
  "message": "Lettere non valide"
}
```
- `errorType` matches the frontend's frozen `ValidationErrorType` union exactly: `TOO_SHORT | MISSING_CENTER | INVALID_LETTERS | ALREADY_FOUND | NOT_IN_DICTIONARY`. Note that `ALREADY_FOUND` cannot occur on this stateless endpoint (no server-side found-words tracking for guests) — it only ever appears on the authenticated `POST /api/v1/game/submit`.
- **Response `404 Not Found`** if `date` has no generated puzzle and is not today (the server will only lazily generate **today's** puzzle, never arbitrary past/future dates).

---

## 2. Authentication & Sync (Guest → Account)

### `POST /api/v1/auth/register`
Creates a new account. Accepts an optional `guestData` payload to migrate locally-stored guest progress into the new account — see `sync-strategy.md` for the full merge algorithm.

- **Request Body:**
```json
{
  "username": "mario_rossi",
  "email": "mario@example.com",
  "password": "PasswordSicura123!",
  "guestData": {
    "history": [
      {
        "date": "2026-08-28",
        "foundWords": ["PORTARE", "RIPORTARE"],
        "invalidWords": ["CASA"],
        "startTime": 1787910000000,
        "lastUpdated": 1787913600000
      }
    ],
    "stats": {
      "gamesPlayed": 1,
      "gamesCompleted": 0,
      "currentStreak": 1,
      "maxStreak": 1,
      "lastPlayedDate": "2026-08-28"
    }
  }
}
```
> `guestData.stats` is accepted for context/logging only and is **never trusted directly** — see `sync-strategy.md §3` (all stats are recalculated server-side from the validated `history` entries).

- **Response `201 Created`:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "b6e2f0b0-6c1e-4e2a-9c1a-8f2e0b0c6e1e",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "userId": "3f2c9a2e-6b7d-4a3e-9c1a-2b7e4a3e9c1a",
  "username": "mario_rossi",
  "email": "mario@example.com",
  "roles": ["ROLE_USER"]
}
```
- **Response `409 Conflict`** if `username` or `email` is already taken.

### `POST /api/v1/auth/login`
Authenticates an existing user and returns a fresh token pair.

- **Request Body:**
```json
{ "username": "mario_rossi", "password": "PasswordSicura123!" }
```
- **Response `200 OK`:** same shape as the registration response above.
- **Response `401 Unauthorized`** on bad credentials.

### `POST /api/v1/auth/refresh`
Exchanges a valid, non-revoked refresh token for a new access token (and a rotated refresh token).

- **Request Body:**
```json
{ "refreshToken": "b6e2f0b0-6c1e-4e2a-9c1a-8f2e0b0c6e1e" }
```
- **Response `200 OK`:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```
- **Response `401 Unauthorized`** if the refresh token is expired, unknown, or `revoked = true`.

### `POST /api/v1/auth/logout`
Revokes the given refresh token, so it can never be exchanged again. The current access token is left to expire naturally (standard stateless-JWT trade-off, see `backend-architecture.md §5`).

- **Headers:** `Authorization: Bearer <ACCESS_JWT_TOKEN>`
- **Request Body:**
```json
{ "refreshToken": "b6e2f0b0-6c1e-4e2a-9c1a-8f2e0b0c6e1e" }
```
- **Response `204 No Content`**

---

## 3. Game & Stats (Authenticated User)

### `GET /api/v1/game/state?date=YYYY-MM-DD`
Retrieves the saved game state for the authenticated user, for a specific date.

- **Headers:** `Authorization: Bearer <ACCESS_JWT_TOKEN>`
- **Response `200 OK`:**
```json
{
  "date": "2026-08-28",
  "foundWords": ["PORTARE", "RIPORTARE"],
  "invalidWords": ["CASA"],
  "currentScore": 23,
  "rankLabel": "🍃 Mente Fresca",
  "isCompleted": false,
  "startTime": 1787910000000,
  "lastUpdated": 1787913600000
}
```
- **Response `404 Not Found`** if the user has no game state for that date (client should treat this identically to "fresh game," matching the frontend's own "no saved state" fallback behavior).

### `POST /api/v1/game/submit`
Submits a word for the authenticated user. Calculates points and atomically updates both `GameState` and `PlayerStats` in the database.

- **Headers:** `Authorization: Bearer <ACCESS_JWT_TOKEN>`
- **Request Body:**
```json
{ "date": "2026-08-28", "word": "RIPORTARE" }
```
- **Response `200 OK`:**
```json
{
  "word": "RIPORTARE",
  "isValid": true,
  "pointsAwarded": 16,
  "isMielegramma": true,
  "currentScore": 23,
  "rankLabel": "🍃 Mente Fresca",
  "isCompleted": false,
  "message": "Mielegramma trovato!"
}
```
- On `isValid: false`, the response also includes `errorType` (same union as `validate-word`), and the word is recorded into `game_state_invalid_words` unless `errorType` is `ALREADY_FOUND` — mirroring the frontend's own `submitWord()` rule exactly.
- This is the **only** endpoint that mutates `player_stats` / `player_seasons` for an authenticated user during normal gameplay (via `StatsService.recordProgress`, same semantics as the frontend service of the same name).

### `GET /api/v1/stats/me`
Retrieves the authenticated user's career stats, current streak, and rank distribution.

- **Headers:** `Authorization: Bearer <ACCESS_JWT_TOKEN>`
- **Response `200 OK`:**
```json
{
  "gamesPlayed": 12,
  "gamesCompleted": 5,
  "currentStreak": 4,
  "maxStreak": 7,
  "lastPlayedDate": "2026-08-28",
  "currentSeason": {
    "year": 2026,
    "totalSeasonPoints": 1450,
    "basePointsEarned": 1100,
    "bonusStreakPoints": 350,
    "highestTierAchieved": "Ape Regina"
  },
  "dailyRankDistribution": {
    "🌱 Iniziato": 1,
    "🍃 Mente Fresca": 3,
    "🧠 Genio": 3,
    "🐝 Ape Regina": 5
  }
}
```

---

## 4. Backoffice Administration (`ROLE_ADMIN`)

### `POST /api/v1/admin/dictionary/words`
Adds a new word to the official dictionary.

- **Headers:** `Authorization: Bearer <ACCESS_JWT_TOKEN_ADMIN>`
- **Request Body:**
```json
{ "word": "BEESAGONO" }
```
- **Response `201 Created`:**
```json
{
  "id": "9c1a2b7e-4a3e-4c9c-8a2e-6b7d3f2c9a2e",
  "word": "BEESAGONO",
  "length": 9,
  "uniqueLettersCount": 7,
  "isCandidatePangram": true,
  "addedByAdmin": true
}
```
- **Response `409 Conflict`** if the word already exists.
- **Response `403 Forbidden`** if the caller does not hold `ROLE_ADMIN`.
- Adding a word does **not** retroactively affect any already-generated `daily_puzzles` row (immutability rule, see `backend-architecture.md §4`) — it only becomes eligible for **future** puzzle generations.