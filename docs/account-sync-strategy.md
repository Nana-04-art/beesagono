# Guest ↔ Account Sync Strategy — Beesagono

This document defines how locally-stored guest progress (browser `localStorage`) is reconciled with the server database when a guest registers, logs in, or plays across multiple devices. It complements `api-contracts.md §2` and `backend-data-model.md`.

---

## 1. Background: What the Guest Client Has Stored

While playing as a **Guest**, the Angular frontend stores everything locally, namespaced as documented in the frontend's own `data-models.md`:
- `beesagono:game:{YYYY-MM-DD}` → one `GameState` per day played (`foundWords`, `invalidWords`, `score`, `isCompleted`, `startTime`, `lastUpdated`)
- `beesagono:stats` → one aggregate `PlayerStats` blob

None of this has ever been validated server-side — a guest's browser is fully in control of what it reports, so **nothing coming from `guestData` is trusted at face value**. The backend's job during sync is to re-derive everything it can from first principles and only accept what it can independently verify.

---

## 2. Trigger Points

| Trigger | Endpoint | Scope of merge |
| :--- | :--- | :--- |
| First-time registration with local guest history | `POST /api/v1/auth/register` (with `guestData`) | One-time: merges the guest's entire local history into the brand-new account. |
| Login from a device with pre-existing local guest/offline data | `POST /api/v1/auth/login` (extended with an optional `guestData` payload, same shape as registration) | Recurring: merges any locally-cached days not yet present on the server for this user. |

Both flows share the same underlying merge algorithm (Sections 3–5); registration is simply the special case where the server-side history starts empty.

---

## 3. Step 1 — Validate & Sanitize Incoming Data

For every entry in `guestData.history`:

1. **Date format check:** `date` must match `YYYY-MM-DD` and be a real calendar date (leap years, month/day ranges) — reject the entire request with `422 Unprocessable Entity` if any entry fails this, so a malformed payload can't silently corrupt a subset of the merge.
2. **Puzzle existence check:** the server must have (or be able to lazily generate, if the date is *today*) a `daily_puzzles` row for that date. Entries for dates the server cannot produce a puzzle for (e.g. a `date` in the future, or too far in the past before the backend existed) are **dropped** — logged, not merged, request otherwise proceeds.
3. **Word-list integrity check:** every word in `foundWords` is checked against that date's `puzzle_possible_words`. **Any word not in the official list is discarded** — this is the primary anti-cheat gate, preventing a tampered browser session from injecting fabricated finds (and therefore fabricated points) into a real account.
4. **`invalidWords` are informational only** — they don't affect score or stats and are merged as-is (union) without validation beyond basic string sanity, since they can never grant points.
5. `guestData.stats` (the aggregate totals block) is accepted **for logging/telemetry purposes only** and is never written directly to `player_stats` — see Section 5.

---

## 4. Step 2 — Per-Date Conflict Resolution (`game_states`)

For each **sanitized** history entry, check whether a `game_states` row already exists for `(user_id, puzzle_id)`:

- **No existing row:** insert a new `game_states` row (plus `game_state_found_words` / `game_state_invalid_words`) directly from the sanitized entry. `current_score` is **not** taken from the client — it is recomputed server-side from the sanitized `foundWords` list using the same point rules as `ScoreService` (word length + mielegramma bonus), guaranteeing the persisted score always matches what the persisted word list would independently produce.
- **Existing row for the same date (e.g. the account was already used on another device for that day):** apply, in order:
  1. **Best-effort union of finds:** `foundWords_merged = foundWords_server ∪ foundWords_guest` (Set union) — a word found on *either* source is kept. This is safe specifically because every word has already passed the Section 3 dictionary-membership check on both sides, so the union can't reintroduce a fabricated word.
  2. **Score recomputation, not comparison:** rather than the naive "keep whichever `current_score` is higher" rule from the original draft — which is fragile, since a higher *reported* score doesn't necessarily correspond to a valid word list — the backend **always recomputes `current_score` from `foundWords_merged`** using `ScoreService`. This subsumes the "higher score wins" intent (a strict superset of found words can only ever score the same or higher) while remaining tamper-proof.
  3. `is_completed` is recomputed as `foundWords_merged.size == puzzle_possible_words.size` for that puzzle.
  4. `last_updated` is set to `max(last_updated_server, last_updated_guest)`.

This step runs inside a single `@Transactional` boundary per user so a partial merge can never leave `game_states` and its child word tables inconsistent.

---

## 5. Step 3 — Deterministic Recalculation of Stats & Streaks

Rather than trusting any totals sent by the client, `player_stats` / `player_seasons` / `player_season_milestones` / `player_daily_rank_distribution` are **fully rebuilt from the validated game history**, mirroring exactly how the frontend's own `StatsService.rebuildStatsFromStorage()` reconstructs stats from raw `GameState` entries when its aggregate blob is missing:

1. Load **all** of the user's `game_states` rows (both pre-existing and newly merged in Step 2), sorted chronologically by the parent puzzle's `puzzle_date`.
2. Replay them in order, exactly as `StatsService` does client-side:
   - `games_played` += 1 per distinct date; `games_completed` += 1 where `is_completed = true`.
   - `current_streak` / `max_streak`: increment on exactly one calendar day after the previous processed date; reset to 1 otherwise (`diffDays == 1` check, UTC-day arithmetic to avoid DST edge cases).
   - `player_seasons` (grouped by the year of `puzzle_date`): `base_points_earned` accumulates each day's `current_score`; streak milestones (3/7/15/30/50/100/200/365, see `backend-data-model.md §5`) award their one-time `bonus_streak_points`, tracked per-season in `player_season_milestones` to prevent double-awarding across repeated syncs.
   - `total_season_points = base_points_earned + bonus_streak_points`.
   - `highest_tier_achieved` resolved from `CAREER_TIERS` against `total_season_points` relative to a day-of-year-scaled expectation, same formula as the frontend.
   - `player_daily_rank_distribution` incremented once per day using the rank label resolved from that day's final `current_score / max_score`.
3. Persist the freshly computed aggregates, **overwriting** any prior `player_stats` row for the user (idempotent — re-running the same sync twice produces the same result, since it always rebuilds from the full validated history rather than incrementally applying deltas).

> This makes stats **impossible to inflate via a crafted `guestData.stats` payload** — the block is read for logging purposes only and has zero influence on the persisted numbers.

---

## 6. Multi-Device Sync (Returning User, New Device)

When an already-registered user logs in from a **new device** that also has its own locally-cached guest/offline play (e.g. played a few days on a phone before ever logging in on that phone):

1. **Payload:** the login request includes the same `guestData.history` shape as registration, containing whatever days are cached locally on that device.
2. **Non-overlapping dates:** for any date present on the device but **absent** on the server for this user, the backend inserts it directly (Step 2's "no existing row" path) — this is how offline play on a phone, potentially for past or non-consecutive dates relative to the user's last PC session, gets folded into the account.
3. **Overlapping dates:** for any date present on **both** the device and the server, Step 2's "existing row" merge path applies (Set-union of found words, recomputed score, `last_updated = max(...)`).
4. **Stats rebuild:** Section 5's full deterministic rebuild always runs after any merge, regardless of how many dates were new vs. overlapping — this guarantees `current_streak` correctly reflects the *true* chronological sequence of all now-known play dates, including ones that were just backfilled from the device, even if they're not adjacent to the most recent server-known date.

### End Result
A player can freely move between devices — logging in on a new phone, playing offline on a spotty connection, then syncing later — without losing a single validated found word, and without any client-reported total ever being trusted over what the server can independently reconstruct from the (per-word) validated history.

---

## 7. Why the Backend Is the Single Authority

This sync design only works because puzzle generation, the dictionary, and score/rank math have all moved server-side (see `backend-architecture.md §4`):

- **Zero puzzle discrepancies:** every device reads the same `daily_puzzles` row from the DB, so there is no risk of two devices disagreeing on what today's `possibleWords`/`mielegrammi` even are.
- **Lighter frontend:** the Angular client no longer ships or parses the full `dictionary.json`; it only ever needs the day's 7 letters and `maxScore` from `GET /api/v1/puzzle/today`.
- **Anti-cheat by construction:** because `possibleWords` and scoring rules never reach the browser in bulk, and every guest-history word is checked against them at sync time, a modified `localStorage` payload can, at worst, be silently dropped — it can never inject unearned points into an account.