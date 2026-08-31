# API Contracts & JSON Schemas — Beesagono Backend

REST API contract for the Beesagono backend. All endpoints are versioned under `/api/v1`. All entity identifiers are **UUID strings** (`varchar(36)`), matching the MySQL 8.0+ schema defined in `backend-data-model.md`.

---

## 0. Conventions

### 0.1 Authentication

Protected endpoints require the JWT access token passed via the HTTP Authorization header:

```http
Authorization: Bearer <ACCESS_JWT_TOKEN>

```

### 0.2 Standard Error Envelope

All error responses (4xx/5xx) share a single shape, in the style of RFC 7807 `ProblemDetail`:

```json
{
  "timestamp": "2026-08-31T14:32:10Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "La password deve contenere almeno 8 caratteri.",
  "path": "/api/v1/auth/register"
}

```

| HTTP Status | `error` Code | Typical Cause |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Malformed/missing request fields (Jakarta Validation failure) |
| 401 | `UNAUTHORIZED` | Missing, invalid, or expired access token |
| 403 | `FORBIDDEN` | Valid token but insufficient role (e.g. non-admin calling `/admin/**`) |
| 404 | `NOT_FOUND` | Resource does not exist (e.g. no `game_sessions` for the given date) |
| 409 | `CONFLICT` | Unique-constraint violation (e.g. `username`/`email` already registered) |
| 422 | `UNPROCESSABLE_ENTITY` | Well-formed but semantically invalid input (e.g. invalid date format) |
| 500 | `INTERNAL_ERROR` | Unexpected server fault |

### 0.3 Dates & Content-Type

* All dates use ISO format `YYYY-MM-DD`, evaluated against the **server's local date**, matching the frontend's local-date policy for daily rollover.
* Requests with a body must include `Content-Type: application/json`.

---

## 1. Authentication & Sync (`/api/v1/auth`)

### `POST /api/v1/auth/register`

Creates a new user account and optionally merges local guest progress stored in `localStorage`.

* **Headers:** `Content-Type: application/json`
* **Request Body:**

```json
{
  "username": "mario_rossi",
  "email": "mario.rossi@example.com",
  "password": "PasswordSicura123!",
  "guestData": {
    "history": [
      {
        "date": "2026-08-30",
        "foundWords": ["POETA", "PAROLE", "RIPORTARE"],
        "invalidWords": ["CASA", "PROVA"],
        "startTime": "2026-08-30T08:30:00Z",
        "lastUpdated": "2026-08-30T09:15:22Z"
      }
    ]
  }
}

```

* **Response `201 Created`:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "d8e9f0a1-2b3c-4d5e-6f7a-8b9c0d1e2f3a",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "a3b1c2d3-e4f5-6a7b-8c9d-0e1f2a3b4c5d",
    "username": "mario_rossi",
    "email": "mario.rossi@example.com",
    "roles": ["ROLE_USER"]
  },
  "syncSummary": {
    "sessionsMerged": 1,
    "wordsAccepted": 3,
    "wordsRejected": 0
  }
}

```

---

### `POST /api/v1/auth/login`

Authenticates an existing user, returns fresh JWT tokens, and merges any unsynced local guest data.

* **Headers:** `Content-Type: application/json`
* **Request Body:**

```json
{
  "usernameOrEmail": "mario_rossi",
  "password": "PasswordSicura123!",
  "guestData": null
}

```

* **Response `200 OK`:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "d8e9f0a1-2b3c-4d5e-6f7a-8b9c0d1e2f3a",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "a3b1c2d3-e4f5-6a7b-8c9d-0e1f2a3b4c5d",
    "username": "mario_rossi",
    "email": "mario.rossi@example.com",
    "roles": ["ROLE_USER"]
  }
}

```

---

### `POST /api/v1/auth/refresh`

Exchanges a valid, non-revoked refresh token for a new access token.

* **Headers:** `Content-Type: application/json`
* **Request Body:**

```json
{
  "refreshToken": "d8e9f0a1-2b3c-4d5e-6f7a-8b9c0d1e2f3a"
}

```

* **Response `200 OK`:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "e9f0a1b2-3c4d-5e6f-7a8b-9c0d1e2f3a4b",
  "tokenType": "Bearer",
  "expiresIn": 900
}

```

---

### `POST /api/v1/auth/logout`

Revokes the specified refresh token in the database (`revoked = true`).

* **Headers:**
* `Authorization: Bearer <ACCESS_JWT_TOKEN>`
* `Content-Type: application/json`


* **Request Body:**

```json
{
  "refreshToken": "e9f0a1b2-3c4d-5e6f-7a8b-9c0d1e2f3a4b"
}

```

* **Response `204 No Content**`

---

## 2. Public & Game Engine (`/api/v1/puzzle`)

### `GET /api/v1/puzzle/today`

Retrieves today's puzzle. Generates and persists it if not yet initialized.

* **Headers:** None (Public)
* **Response `200 OK`:**

```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d4e5",
  "date": "2026-08-31",
  "seed": "5381_123",
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

---

### `POST /api/v1/puzzle/validate-word`

Validates a word for a guest session without persisting state to the database.

* **Headers:** `Content-Type: application/json`
* **Request Body:**

```json
{
  "date": "2026-08-31",
  "word": "RIPORTARE"
}

```

* **Response `200 OK` (Valid Word):**

```json
{
  "word": "RIPORTARE",
  "isValid": true,
  "pointsAwarded": 16,
  "isMielegramma": true,
  "errorType": null,
  "message": "Mielegramma trovato! 🐝"
}

```

* **Response `200 OK` (Invalid Word):**

```json
{
  "word": "CASA",
  "isValid": false,
  "pointsAwarded": 0,
  "isMielegramma": false,
  "errorType": "MISSING_CENTER",
  "message": "La parola non contiene la lettera centrale 'I'."
}

```

---

## 3. Game & Stats (`/api/v1/game` & `/api/v1/stats`)

### `GET /api/v1/game/state?date=YYYY-MM-DD`

Retrieves the saved game session (`game_sessions`) for the authenticated user on a specific date.

* **Headers:** `Authorization: Bearer <ACCESS_JWT_TOKEN>`
* **Response `200 OK`:**

```json
{
  "sessionId": "c9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
  "date": "2026-08-31",
  "currentScore": 48,
  "currentRankLabel": "Stupendo 😀",
  "isCompleted": false,
  "foundWords": [
    {
      "word": "POETA",
      "foundAt": "2026-08-31T09:10:00Z",
      "points": 5,
      "isMielegramma": false
    },
    {
      "word": "RIPORTARE",
      "foundAt": "2026-08-31T09:12:30Z",
      "points": 16,
      "isMielegramma": true
    }
  ],
  "invalidAttemptsCount": 2,
  "startTime": "2026-08-31T09:00:00Z",
  "lastUpdated": "2026-08-31T09:12:30Z"
}

```

---

### `POST /api/v1/game/submit`

Submits a word for the authenticated user, updates `game_sessions`, `found_words`, and `player_stats` atomically.

* **Headers:**
* `Authorization: Bearer <ACCESS_JWT_TOKEN>`
* `Content-Type: application/json`


* **Request Body:**

```json
{
  "date": "2026-08-31",
  "word": "PAROLE"
}

```

* **Response `200 OK`:**

```json
{
  "word": "PAROLE",
  "accepted": true,
  "pointsAwarded": 6,
  "isMielegramma": false,
  "sessionState": {
    "currentScore": 54,
    "currentRankLabel": "Stupendo 😀",
    "totalFoundWords": 3,
    "isCompleted": false
  }
}

```

---

### `GET /api/v1/stats/me`

Retrieves career statistics, streaks, current season progression, and rank distributions for the authenticated user.

* **Headers:** `Authorization: Bearer <ACCESS_JWT_TOKEN>`
* **Response `200 OK`:**

```json
{
  "userId": "a3b1c2d3-e4f5-6a7b-8c9d-0e1f2a3b4c5d",
  "currentStreak": 5,
  "maxStreak": 14,
  "careerPoints": 12450,
  "gamesPlayed": 42,
  "gamesCompleted": 18,
  "lastPlayedDate": "2026-08-31",
  "currentSeason": {
    "year": 2026,
    "basePoints": 11450,
    "bonusPoints": 1000,
    "totalPoints": 12450,
    "tier": "🐝 Ape Regina"
  },
  "rankHistogram": [
    { "rankLabel": "🌱 Iniziato", "count": 0 },
    { "rankLabel": "🍃 Mente Fresca", "count": 2 },
    { "rankLabel": "🐣 Principiante", "count": 5 },
    { "rankLabel": "🚀 Avanzato", "count": 8 },
    { "rankLabel": "💡 Esperto", "count": 7 },
    { "rankLabel": "⭐ Eccellente", "count": 6 },
    { "rankLabel": "🧠 Genio", "count": 10 },
    { "rankLabel": "👑 Maestro", "count": 3 },
    { "rankLabel": "🐝 Ape Regina", "count": 1 }
  ]
}

```

---

## 4. Backoffice Administration (`/api/v1/admin`)

### `POST /api/v1/admin/dictionary/words`

Adds a new dictionary word to `dictionary_words`. Requires `ROLE_ADMIN`.

* **Headers:**
* `Authorization: Bearer <ADMIN_JWT_TOKEN>`
* `Content-Type: application/json`


* **Request Body:**

```json
{
  "word": "BEESAGONO",
  "isCandidatePangram": false
}

```

* **Response `201 Created`:**

```json
{
  "word": "BEESAGONO",
  "wordLength": 9,
  "uniqueLettersCount": 7,
  "isCandidatePangram": false,
  "addedByUserId": "a3b1c2d3-e4f5-6a7b-8c9d-0e1f2a3b4c5d",
  "addedAt": "2026-08-31T11:00:00Z"
}

```

* **Response `403 Forbidden`:**

```json
{
  "timestamp": "2026-08-31T11:01:00Z",
  "status": 403,
  "error": "FORBIDDEN",
  "message": "Accesso negato: richiesto privilegio ROLE_ADMIN.",
  "path": "/api/v1/admin/dictionary/words"
}

```