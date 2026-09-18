# Sweep

Sweep is a 3-player, sum-to-15 card-capture game (in the same family as Scopa/Cassino/Xeri), built with **libGDX** as a cross-platform client. It can be played fully offline (local AI-free "singleplayer" with 3 hardcoded seats) or online against real players, in which case this client talks to a separate Spring Boot backend: **[Sweep-Server](../Sweep-Server)**.

This document explains what lives in this repo, what each piece does, exactly how it talks to its counterpart in `Sweep-Server`, and how to boot both up locally.

> **Heads-up:** game rules, timers, speeds, scoring and game-modes are actively being tweaked, and the codebase has known rough edges (see [Known gaps / bugs](#known-gaps--bugs-observed-in-the-code)). Treat the "Game rules" section below as "what the code currently does", not a frozen spec.

## Table of contents
- [Repo layout](#repo-layout)
- [Module reference](#module-reference)
- [Game rules (current implementation)](#game-rules-current-implementation)
- [Client ↔ Sweep-Server connection map](#client--sweep-server-connection-map)
- [Known gaps / bugs observed in the code](#known-gaps--bugs-observed-in-the-code)
- [Booting everything up](#booting-everything-up)
- [Running tests](#running-tests)

## Repo layout

This is a multi-module Gradle project (see `settings.gradle`):

```
Sweep-Base/
├── game-logic/   shared rules engine — also used, unmodified, by Sweep-Server
├── core/         platform-agnostic libGDX app: screens, UI, networking client
├── lwjgl3/       desktop launcher (Windows/Linux/macOS)
├── android/      Android launcher + assets (assets/ lives under here)
├── ios/          iOS launcher (RoboVM)
├── html/         GWT/web launcher
└── assets/       card art, fonts, UI skin (mirrored into html/war at build time)
```

`Sweep-Server` is a **sibling repository/folder**, not a subfolder of this one — see [why](#the-shared-game-logic-module) below.

## Module reference

| Module | What it is | Key classes |
|---|---|---|
| `game-logic` | Pure-Java rules engine with no libGDX/Spring dependency. Compiled as its own jar and reused by both the client and the server so both sides enforce identical rules. | `Card`, `Deck`, `Rank`, `Suit`, `Player`, `SweepLogic` |
| `core` | The actual game: all screens, all UI widgets, the HTTP auth client, and the STOMP/WebSocket client. Every platform launcher just boots this module. | `SweepGame` (libGDX `Game` entry point), `game/*` (game-mode glue), `ui/*` (screens), `network/*` (server communication) |
| `lwjgl3` | Desktop launcher (`Lwjgl3Launcher`), window/vsync config. This is the fastest way to iterate locally. | `Lwjgl3Launcher` |
| `android` | Android `Activity` wrapper (`AndroidLauncher`) + the canonical `assets/` folder that `lwjgl3` and `html` both borrow from at build time. | `AndroidLauncher` |
| `ios` | RoboVM-based iOS launcher. Can only be built/run from macOS. | `IOSLauncher` |
| `html` | GWT transpiles `core` (and this module's sources) to JS; served via the `html/webapp` war. | `GwtLauncher` |

### `core/game` — game-mode abstraction
- `GameMode` — interface both game modes implement (`startGame`, `playCard`, `isGameOver`, `getWinner`, `isMyTurn`, ...). This is what the screens (`SweepGameUI`, `HandUI`, `TableUI`) actually program against, so the UI doesn't care whether it's local or networked.
- `SingleplayerMode` — wraps a local `SweepLogic` instance directly; always "my turn"; three fixed player names (`Johnny`, `Joni`, `Rodrigo`) are created inside `SweepLogic.startGame()`.
- `MultiplayerMode` — wraps a `WebSocketManager` connection. It never runs game rules itself — it only sends move intents to the server and rehydrates its player list/table/turn from the `GameStateDTO` broadcasts the server pushes back. Implements `WebSocketManager.GameStateListener` to receive those pushes.
- `DifficultyConfig` — singleplayer-only: decides whether an empty-selection play auto-solves a 15-combo ("Easy") and whether a per-turn countdown applies ("Hard" = 15s, "Pedrado" = 10s). Has no effect in multiplayer.
- `TournamentManager` — singleplayer-only best-of series tracker (`single`, `first_to_4`, `first_to_8`) and anti-clockwise starting-seat rotation between games. Singleton; reset when returning to the home screen.
- `SweepGameUI` — the actual game screen (`Screen` implementation). Has two constructors: one for singleplayer (builds a `SingleplayerMode` + wires up `TournamentManager`/`DifficultyConfig`) and one for multiplayer (takes an already-connected `GameMode`).

### `core/network` — talks to Sweep-Server
- `AuthService` — plain HTTP (via `Gdx.net`) to the server's REST auth endpoints. Persists `accessToken`/`refreshToken`/`username` in libGDX `Preferences` under `SweepAuth`, and runs a repeating `Timer.Task` every 14 minutes to silently refresh the access token.
- `WebSocketManager` — STOMP-over-SockJS client (using Spring's `spring-websocket`/`spring-messaging` client libraries bundled into `core`, plus a Tyrus WebSocket implementation) that connects to the server's `/ws` endpoint, authenticates the STOMP `CONNECT` frame with a `Bearer` header, subscribes to per-user queues, and exposes `joinQueue`/`leaveQueue`/`playCard`/`sendReady` senders plus a `GameStateListener` callback interface.
- `network/dto/*` (`LoginRequestDTO`, `RegisterRequestDTO`, `AuthResponseDTO`) and `network/GameStateDTO` / `network/PlayerStateDTO` — client-side copies of the JSON shapes the server sends/expects. They are **hand-maintained duplicates** of the server's own DTO classes (see [Known gaps](#known-gaps--bugs-observed-in-the-code)).

### `core/ui` — screen flow
```
WelcomeScreenUI ── Login ──► LoginScreenUI ─────┐
       │                                        ▼
       └── Register ──► RegisterScreenUI ──► HomeScreenUI
                                                 │
                     ┌───────────────────────────┼───────────────────────────┐
                     ▼                                                       ▼
     SingleplayerModeSelectionUI                                  MultiplayerMenuUI
                     │                                                       │
        SingleplayerDifficultyUI                                 MatchmakingQueueUI
                     │                                                       │
                     └──────────────────► SweepGameUI ◄───────────────────────┘
```
`RulesScreenUI` is reachable from the home screen and contains the in-app copy of the rules (also summarized below). `ScoreUI`, `TableUI`, `HandUI`, `PlayerSeatUI` are the modular sub-widgets `SweepGameUI` composes together (score labels, table cards, the local player's hand, and the two opponent seats).

### `core/utils`
- `LayoutHelper` — singleton that picks a virtual viewport size (desktop/web vs. mobile) and provides `scale()` for UI sizing.
- `FontManager` — singleton that generates the TTF-based fonts (`lsans.ttf`) used to replace the skin's default bitmap font everywhere.

## Game rules (current implementation)

Taken from `game-logic/SweepLogic` and the in-app `RulesScreenUI` copy — **subject to change**:

- **Deck**: 40 cards — 4 suits × {Ace, 2, 3, 4, 5, 6, 7, Jack, Queen, King}. Values for summing: Ace = 1, numbers = face value, **Queen = 8, Jack = 9, King = 10** (note the Jack/Queen value swap vs. a standard deck).
- **Players**: fixed at 3 (singleplayer names are hardcoded; multiplayer seats are the 3 matched real players).
- **Deal**: 3 cards to each player + 4 to the table; redeal 3-per-player whenever every hand is empty and the deck isn't exhausted; game ends when the deck is empty and hands are empty.
- **Turn**: play one card from your hand, optionally selecting table cards. If the played card's value plus the selected table cards sum to exactly 15, you capture all of them into your points stack. If nothing is selected and there's a valid 15 combination lying around, in "Medium/Hard/Pedrado" difficulty the card is simply placed on the table (no forced/auto capture) — only "Easy" auto-solves for you.
- **Sweep ("brush")**: capturing every card currently on the table in one move grants a sweep bonus (tracked separately via `Player.incrementBrushes()`), plus whatever the 15-combination itself was worth.
- **Special first-round rule**: if the initial 4 table cards already sum to 15, the starting player automatically sweeps them before anyone plays.
- **Scoring**: each diamond = 1 point, each 7 = 1 point, the 7 of diamonds = 2 points (not stacked to 3). Winner = most points; ties are broken by whoever captured more total cards, else at random.
- **Singleplayer difficulties** (`DifficultyConfig`): Easy = auto-select a random valid 15-combo on an empty-selection play, no timer. Medium = no auto-select, no timer. Hard = no auto-select, 15s per-turn timer. Pedrado = no auto-select, 10s per-turn timer.
- **Tournaments** (`TournamentManager`, singleplayer only): single game, first-to-4, or first-to-8 wins; the starting seat rotates anti-clockwise (0 → 2 → 1 → 0) each game.
- **Multiplayer** currently has none of the above difficulty/timer/tournament layer — it's always a single untimed game with the server always starting at seat index 0.

## Client ↔ Sweep-Server connection map

### The shared `game-logic` module
Both repos compile the exact same `game-logic` sources. `Sweep-Server/settings.gradle` includes it via a **relative path**:
```groovy
project(':game-logic').projectDir = new File('../Sweep-Base/game-logic')
```
This is why `Sweep-Base` and `Sweep-Server` must be checked out as **sibling directories** (e.g. both directly under `D:\Sweep\`) — the server's build literally reaches into this repo's folder. The server runs the authoritative `SweepLogic` per game session; the client's `SingleplayerMode` runs its own independent local instance of the same class; `MultiplayerMode` never touches `SweepLogic` at all.

### REST — authentication & profile
| Client | Protocol | Server |
|---|---|---|
| `network/AuthService` → `POST /api/auth/register`, `/login`, `/refresh` | HTTP + JSON (Gson) | `AuthController` → `AuthService` → `UserRepository` / `JwtConfig` |
| *(no client caller found)* | HTTP + JSON | `UserController` → `/api/user/profile` (GET/PUT), `/stats`, `/history`, `/history/wins`, `/delete` |

Access tokens expire in 15 minutes (`jwt.access-token.expiration`); refresh tokens last 30 days (browser) or 1 year (mobile/desktop) per `application.properties`. The client sends an `X-Platform` header (`mobile`/`desktop`/`browser`) so the server knows which refresh-token lifetime to issue.

### WebSocket/STOMP — live gameplay
Client `WebSocketManager` connects to the server's `/ws` SockJS+STOMP endpoint (registered in server `WebSocketConfig`), passing `Authorization: Bearer <accessToken>` on the STOMP `CONNECT` frame. The server's `configureClientInboundChannel` interceptor reads that header, extracts the username via `JwtConfig`, and attaches it as the STOMP session's `Principal` — this is a lighter-weight, separate auth path from the REST JWT filter (`JwtAuthenticationFilter`), it does not go through Spring Security's normal chain.

Client → server (`@MessageMapping` in `GameController`):

| Client call | Destination | Server handler | Does |
|---|---|---|---|
| `wsManager.joinQueue(ranked)` | `/app/game/join` | `joinMatchmaking` | Adds you to `MatchmakingService`'s casual/ranked queue; once 3 players are queued, `GameSessionManager` creates a session and the game starts immediately |
| `wsManager.leaveQueue()` | `/app/game/leave` | `leaveMatchmaking` | Removes you from both queues and from any session |
| `wsManager.playCard(handIdx, tableIdxs)` | `/app/game/move` | `playCard` | Validates turn order, looks up the real `Card`s by index, calls the shared `SweepLogic.playCardWithSelection` |
| `wsManager.sendReady()` | `/app/game/ready` | `playerReady` | Marks a player ready; starts the session once all are ready — **not currently invoked by any screen in this client** |

Server → client (subscriptions set up in `WebSocketManager.subscribeToTopics`):

| Topic | Payload | Consumed by |
|---|---|---|
| `/user/queue/game-state` | `GameStateDTO` (session id, game state, current player index, table cards, per-player summaries) | `MultiplayerMode.onGameStateUpdate` — rebuilds player list/turn/game-over state |
| `/user/queue/matchmaking` | raw map `{status: "waiting", queueSize}` or `{status: "matched", sessionId}` | `MatchmakingQueueUI` — updates the queue UI or transitions into `SweepGameUI` |
| `/user/queue/errors` | raw map `{error: "..."}` | any active `GameStateListener.onError` |

Per-player privacy: the server intentionally only ever sends `handSize`/`collectedSize`/`points`/`sweeps` for opponents, never their actual cards — `MultiplayerMode.updatePlayers` explicitly does not (and cannot) populate opponents' hands from the DTO.

### DTOs exist twice
The server and client each define their **own** copies of the same wire types:

| Purpose | Client class | Server class |
|---|---|---|
| Login request | `com.sweepgame.network.dto.LoginRequestDTO` | `com.sweepgame.server.model.dto.LoginRequestDTO` |
| Register request | `com.sweepgame.network.dto.RegisterRequestDTO` | `com.sweepgame.server.model.dto.RegisterRequestDTO` |
| Auth response | `com.sweepgame.network.dto.AuthResponseDTO` | `com.sweepgame.server.model.dto.AuthResponseDTO` |
| Game state push | `com.sweepgame.network.GameStateDTO` / `PlayerStateDTO` | `com.sweepgame.server.model.dto.GameStateDTO` (with a nested `PlayerStateDTO`) |
| Move request | *(built inline as a `Map` in `WebSocketManager.playCard`)* | `com.sweepgame.server.model.dto.MoveDTO` |

They are not shared code — a field added/renamed on one side silently breaks (de)serialization on the other until both are updated by hand.

## Known gaps / bugs observed in the code

- **Stats/history are never written.** `GameController.handleGameEnd` has `// TODO: Save game history to database` and `// TODO: Update player stats` — the `GameHistory`/`PlayerStats` JPA entities and the `UserController` read-endpoints (`/api/user/stats`, `/api/user/history`) exist, but nothing ever populates them, so those endpoints will currently always return zeros/empty lists.
- **Duplicated DTOs**, see above — a common source of "why did deserialization silently drop this field" bugs.
- **No tournament/rotation on the server.** `GameSession.startGame()` always calls `gameLogic.startGame()` (defaults to seat 0); the anti-clockwise starting-seat rotation and best-of-N tournament logic only exist in the singleplayer client path (`TournamentManager`).
- **Matchmaking can proceed without a valid token.** `MultiplayerMenuUI.joinQueue` logs a warning ("Mock Mode") and continues into the queue screen even when `accessToken` is null/empty; the server will simply never authenticate that STOMP session, so failures surface later as generic connection errors rather than an upfront "please log in".
- **No server-side move timer.** `DifficultyConfig`'s Hard/Pedrado countdowns are purely client-side and don't exist for multiplayer at all — nothing stops a player from taking forever.
- **`jwt.secret` is a plaintext placeholder** committed in `Sweep-Server/src/main/resources/application.properties` — fine for local dev, must be overridden (env var / secret manager) for anything shared or deployed.
- **Hardcoded server URLs.** `AuthService.BASE_URL` and `WebSocketManager.serverUrl` both point at `http://localhost:8080/...` with no config/build-variant override — edit both constants if you point the client at a non-local server.

## Booting everything up

### Prerequisites
- JDK 17.
- Docker + Docker Compose (simplest way to get the server + Postgres running), **or** a local PostgreSQL 16 instance.
- This repo (`Sweep-Base`) and `Sweep-Server` checked out as **sibling folders** — the server's Gradle build requires `../Sweep-Base/game-logic` to resolve.
- Android Studio (for the `android` module) / a Mac with Xcode (for `ios`) only if you need those targets.

### 1. Start the server (`Sweep-Server`)

**Option A — Docker Compose (recommended, also brings up Postgres):**
```bash
cd D:\Sweep\Sweep-Server
docker compose up --build
```
This builds the server image (which itself copies in `Sweep-Base/game-logic` — see its `Dockerfile`), and exposes the server on `http://localhost:8080` and Postgres on `localhost:5432` (db `sweepdb`, user/pass `sweepuser`/`sweeppass`).

**Option B — Gradle directly** (requires a Postgres reachable at the URL/credentials in `src/main/resources/application.properties`, or override them via `SPRING_DATASOURCE_*` env vars):
```bash
cd D:\Sweep\Sweep-Server
./gradlew bootRun   # gradlew.bat bootRun on native Windows shells
```

Sanity check once it's up: `POST http://localhost:8080/api/auth/register` with a JSON body should return `accessToken`/`refreshToken`.

### 2. Run the client (`Sweep-Base`)

**Desktop — fastest loop for local development:**
```bash
cd D:\Sweep\Sweep-Base
./gradlew lwjgl3:run
```

**Web (GWT dev mode):**
```bash
./gradlew html:superDev
```
then open the URL it prints in the console (serves `html/webapp`).

**Android:**
```bash
./gradlew android:installDebug
```
(or open the project in Android Studio and run the `android` run configuration; requires a connected device/emulator).

**iOS:** requires macOS + Xcode + the RoboVM toolchain; build the `ios` module there — not buildable from Windows.

If your server isn't on `localhost:8080`, update `BASE_URL` in `core/src/main/java/com/sweepgame/network/AuthService.java` and `serverUrl` in `core/src/main/java/com/sweepgame/network/WebSocketManager.java` before building.

## Running tests

```bash
# Shared rules engine (also exercises the exact logic the server runs)
./gradlew game-logic:test

# Server-side tests (in the Sweep-Server repo)
cd D:\Sweep\Sweep-Server
./gradlew test
```
`game-logic` tests: `CardTest`, `SweepLogicTest`. Server tests: `WebSocketConfigTest`, `GameSessionTest`, `GameSessionManagerTest`, `MatchmakingServiceTest`.
