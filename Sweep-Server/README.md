# Sweep-Server

Spring Boot backend for **Sweep**, a 3-player, 15-sum card game (collect cards by matching sums of 15; clearing the table entirely is a "sweep"/"brush"). This server provides account auth, user profiles/stats, matchmaking, and real-time multiplayer game sessions over WebSocket/STOMP for the **Sweep-Base** game client (`D:\Sweep\Sweep-Base`, a libGDX game targeting desktop/Android/iOS/HTML).

> **Status note:** this backend is a work in progress and is not guaranteed to run end-to-end. Several features described below are scaffolded (DB columns, DTO fields, queue types) but not actually wired up yet — see [Known gaps](#known-gaps--incomplete-features).

## How the two repos fit together

`Sweep-Server` and `Sweep-Base` are **sibling directories** and are not independent — the server's Gradle build directly includes a module physically located inside the client repo:

```
settings.gradle:
  include 'game-logic'
  project(':game-logic').projectDir = new File('../Sweep-Base/game-logic')
```

`game-logic` (owned by Sweep-Base) contains the actual rules engine — `SweepLogic`, `Player`, `Card`, `Deck`, `Suit`, `Rank`. Both the client's **singleplayer** mode and the server's **multiplayer** sessions run the exact same `SweepLogic` class, so the server is just running the client's own rules engine, server-side, and streaming state deltas out over WebSocket. This is why `Sweep-Base` must exist as a sibling folder (`D:\Sweep\Sweep-Base`) for `Sweep-Server` to even compile — there's no published artifact, it's a raw path reference.

The Docker build reinforces this: the `Dockerfile`'s build context is `..` (the parent of `Sweep-Server`), and it explicitly copies `Sweep-Base/game-logic` into the image before compiling.

## Architecture at a glance

```
Sweep-Base (client, libGDX)              Sweep-Server (this repo, Spring Boot)
──────────────────────────               ─────────────────────────────────────
core/.../network/AuthService.java   ───► POST /api/auth/register, /login, /refresh
core/.../network/WebSocketManager   ───► STOMP over /ws  (SockJS)
core/.../game/MultiplayerMode       ───► drives a live GameSession server-side
core/.../game/SingleplayerMode      ───  (never talks to the server at all)
core/.../game/TournamentManager     ───  (purely local, no server concept of it)
game-logic/.../SweepLogic           ═══  SAME jar/module used by both sides
```

## Repo layout

```
src/main/java/com/sweepgame/server/
├── SweepServerApplication.java     Spring Boot entry point
├── config/
│   ├── SecurityConfig.java         Stateless JWT security filter chain
│   ├── JwtConfig.java              Token issuing/parsing (jjwt)
│   ├── JwtAuthenticationFilter.java  Reads "Authorization: Bearer ..." on HTTP requests
│   └── WebSocketConfig.java        STOMP broker config + JWT auth on the WS CONNECT frame
├── controller/
│   ├── AuthController.java         /api/auth/** (register, login, refresh)
│   ├── UserController.java         /api/user/** (profile, stats, history, delete)
│   └── GameController.java         STOMP @MessageMapping handlers (matchmaking + gameplay)
├── service/
│   ├── AuthService.java            Registration/login/token-refresh logic, bcrypt
│   ├── UserService.java            Profile/stats/history CRUD via UserRepository
│   ├── MatchmakingService.java     In-memory casual/ranked FIFO queues
│   └── GameSessionManager.java     In-memory registry of active GameSessions
├── model/
│   ├── GameSession.java            Wraps a SweepLogic instance + connected players + state machine
│   ├── PlayerConnection.java       A queued/seated player (username, userId, ws session id, ready flag)
│   └── dto/                        Wire DTOs for WebSocket payloads (GameStateDTO, MoveDTO, ...)
├── entity/                         JPA entities: User, PlayerStats, GameHistory
└── repository/                     Spring Data JPA repositories for User, GameHistory
```

## Client ↔ server counterpart map

| Sweep-Server (this repo) | Sweep-Base (client) counterpart | Connection |
|---|---|---|
| `AuthController` (`/api/auth/register`, `/login`, `/refresh`) | `core/.../network/AuthService.java` | Client POSTs `RegisterRequestDTO`/`LoginRequestDTO` JSON via `Gdx.net`; server returns access+refresh JWTs which the client caches in libGDX `Preferences` ("SweepAuth") and auto-refreshes every 14 minutes |
| `UserController` (`/api/user/**`) | *(no dedicated client class yet)* | Profile/stats/history endpoints exist server-side but nothing in Sweep-Base currently calls them — no profile/stats screen wired to them |
| `WebSocketConfig` (`/ws` STOMP endpoint, SockJS) | `core/.../network/WebSocketManager.java` | Client connects with `Authorization: Bearer <token>` in the STOMP CONNECT header; server's channel interceptor extracts the username from the JWT and sets it as the STOMP `Principal` |
| `GameController#joinMatchmaking` (`/app/game/join`) | `WebSocketManager.joinQueue(isRanked)` → `MultiplayerMenuUI` (Casual/Ranked buttons) | Client sends `{ranked: bool}`; server enqueues via `MatchmakingService` |
| `GameController` queue broadcasts (`/user/queue/matchmaking`) | `WebSocketManager` → `MatchmakingQueueUI` | Server pushes `{status: "waiting", queueSize}`; client's `handleMatchmakingMessage` also expects a `"matched"` status with a `sessionId`, but **the server never sends that status** — see gaps below |
| `GameController#playCard` (`/app/game/move`) | `MultiplayerMode.playCard()` → `WebSocketManager.playCard()` | Client sends `MoveDTO`-shaped payload (hand index + selected table indices); server resolves it against the shared `SweepLogic` and broadcasts the new state |
| `GameController` state broadcasts (`/user/queue/game-state`) | `WebSocketManager` → `MultiplayerMode.onGameStateUpdate()` | Server's `GameStateDTO` (session id, state, current player index, table cards, per-player summaries) is mirrored client-side by `network/GameStateDTO.java` + `PlayerStateDTO.java` |
| `GameController` error channel (`/user/queue/errors`) | `WebSocketManager` → `listener.onError()` | Generic `{error: "..."}` messages (bad move, not your turn, session missing, etc.) |
| `entity/User`, `entity/PlayerStats` | *(no client persistence)* | Server-only; the ranked/stats columns (`rankedPoints`, `winStreak`, `bestWinStreak`, `totalSweeps`) exist in the schema but nothing currently increments them |
| `entity/GameHistory` + `GameHistoryRepository` | *(no client persistence)* | Schema and query methods exist (`findAllGamesByUserId`, `findByWinnerIdOrderByFinishedAtDesc`) but **no code path ever saves a `GameHistory` row** — see gaps below |
| `game-logic` module (`SweepLogic`, `Player`, `Card`, `Deck`) | `core/.../game/SingleplayerMode.java`, `core/.../game/MultiplayerMode.java` | Literally the same classes/module; `SingleplayerMode` runs `SweepLogic` locally, `MultiplayerMode` is a thin client-side shell that mirrors server-pushed state instead of running the logic itself |
| — (no server-side concept) | `core/.../game/TournamentManager.java` | Purely client-side "best of N games" wrapper around repeated singleplayer/multiplayer rounds; the server has no notion of a tournament |

## REST API

All under `/api`, CORS wide open (`@CrossOrigin(origins = "*")`), stateless JWT auth (`SecurityConfig` permits `/api/auth/**` and `/ws/**` without a token; everything else requires a valid `Authorization: Bearer <accessToken>` header).

- `POST /api/auth/register` — body `{username, email, password}`, optional `X-Platform` header (`browser`/`mobile`/`desktop`) controls refresh-token lifetime. Returns `{accessToken, refreshToken, username, message}`.
- `POST /api/auth/login` — body `{username, password}`. Same response shape.
- `POST /api/auth/refresh` — body `{refreshToken}`. Returns a new `{accessToken}`.
- `GET /api/user/profile` / `PUT /api/user/profile` — read/update the authenticated user's profile.
- `GET /api/user/stats` — games played/won, total points, total sweeps, win streak, ranked points.
- `GET /api/user/history` / `GET /api/user/history/wins` — game history (currently always empty, see gaps).
- `DELETE /api/user/delete` — deletes the account.

## WebSocket / STOMP protocol

Endpoint: `/ws` (SockJS-wrapped STOMP, all origins allowed). Auth token is passed as a native STOMP header on CONNECT, not a query param.

**Client → server destinations** (`/app/...`):
- `/app/game/join` — `{ranked: boolean}`
- `/app/game/leave`
- `/app/game/move` — hand card index + selected table card indices
- `/app/game/ready`

**Server → client per-user queues** (`/user/queue/...`):
- `/queue/game-state` — `GameStateDTO`
- `/queue/matchmaking` — queue size updates
- `/queue/errors` — `{error: "message"}`

Matchmaking is a simple FIFO: as soon as 3 players are queued (casual or ranked, tracked as two independent queues), `MatchmakingService` pulls exactly 3 and hands them to `GameSessionManager` to form a `GameSession`.

## Data model

- **`users`** — credentials, email verification fields (unused), OAuth/Google fields (unused, `provider` always `"local"` in practice), one-to-one `PlayerStats`.
- **`player_stats`** — `gamesPlayed`, `gamesWon`, `totalPoints`, `totalSweeps`, `winStreak`, `bestWinStreak`, `rankedPoints`. Created (zeroed) at registration; **never updated afterward**.
- **`game_history`** — three player FKs, winner FK, per-player points, timestamps, duration. Table exists but is never written to.

`spring.jpa.hibernate.ddl-auto=update` — schema is auto-migrated from entities against Postgres on boot.

## Known gaps / incomplete features

This matters for anyone extending the game with new modes, ranks, or leaderboards — the groundwork is partially laid but not finished:

- **Match-found notification never sent.** `GameController.joinMatchmaking` broadcasts `{status:"waiting"}` while queuing, but when a match actually forms it only calls `broadcastGameState(...)` — it never sends the `{status:"matched", sessionId:...}` message that the client's `WebSocketManager.handleMatchmakingMessage` is written to expect. The client-side `onMatchFound` callback appears effectively unreachable via this path.
- **Stats and history are never persisted after a game.** `GameController.handleGameEnd` has explicit `// TODO: Save game history to database` / `// TODO: Update player stats` comments — the ranked-points, win-streak, games-played, and `GameHistory` machinery all exist in the entities but no service call ever writes them.
- **No leaderboard endpoint.** There's a `rankedPoints` column and a ranked matchmaking queue, but no ranking algorithm (ELO/MMR or otherwise) and no `/api/*leaderboard*` route.
- **No additional game modes server-side.** `SingleplayerMode` and `TournamentManager` in Sweep-Base are entirely client-local — the server only knows about one mode: a live 3-player `GameSession` (optionally flagged `isRanked`), and ranked currently behaves identically to casual except for which queue it uses.
- **Sessions vanish on finish.** `GameSessionManager.finishSession` removes the session immediately (`// TODO: Save to database before removing`), so there is no server-side record of a completed game to look up later.
- **Fixed 3-player assumption throughout** (`GameSession.isFull()`, matchmaking threshold) — no support for different table sizes.
- **Security posture is dev-only.** `jwt.secret` in `application.properties` is a placeholder string, CORS/CSRF are disabled, and `/ws/**` is fully unauthenticated at the HTTP layer (auth is only checked inside the STOMP CONNECT frame).
- Test suites include comments flagging real bugs found during testing, e.g. `WebSocketConfigTest` notes a potential NPE if a non-STOMP frame reaches the inbound channel interceptor un-guarded, and `MatchmakingServiceTest` notes `joinQueue` has no guard against the same player being enqueued in both the casual and ranked queues simultaneously.

## Running the server

### Prerequisites

- JDK 17
- The `Sweep-Base` repo checked out as a **sibling directory** — this build will not configure/compile without `D:\Sweep\Sweep-Base\game-logic` present, because of the `settings.gradle` relative path.
- Postgres 16 (or use the bundled `docker-compose.yml`)

### Option A — Docker Compose (server + Postgres)

From `D:\Sweep` (one level above this repo, since the compose file's build context is `..`):

```bash
cd D:\Sweep\Sweep-Server
docker-compose up --build
```

This starts:
- `postgres_sweep` — Postgres 16, db `sweepdb`, user/pass `sweepuser`/`sweeppass`, port `5432`
- `server` — built from `../` context via `Sweep-Server/Dockerfile` (which itself copies in `Sweep-Base/game-logic`), exposed on port `8080`

### Option B — Run locally with Gradle

1. Start a Postgres instance matching `src/main/resources/application.properties` (`sweepdb` / `sweepuser` / `sweeppass` on `localhost:5432`), e.g.:
   ```bash
   docker run -d --name postgres_sweep -p 5432:5432 \
     -e POSTGRES_DB=sweepdb -e POSTGRES_USER=sweepuser -e POSTGRES_PASSWORD=sweeppass \
     postgres:16-alpine
   ```
2. From `D:\Sweep\Sweep-Server`:
   ```bash
   ./gradlew bootRun
   ```
   (Windows: `gradlew.bat bootRun`)
3. Server listens on `http://localhost:8080`; WebSocket endpoint is `http://localhost:8080/ws`. Both are hardcoded as `localhost:8080` in the Sweep-Base client (`AuthService.BASE_URL`, `WebSocketManager.serverUrl`), so no extra client-side config is needed for local testing.

### Running tests

```bash
./gradlew test
```

Covers `GameSession`, `GameSessionManager`, `MatchmakingService`, and `WebSocketConfig` (all pure unit tests with Mockito, no DB/network required). There is currently no test coverage for the controllers, `AuthService`, or `UserService`.
