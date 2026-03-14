# Smart Air Base

Smart Air Base is a turn-based air operations strategy game built with a three-tier architecture.

## Architecture

```
React Frontend (port 3000)
       |
       v
MCPClient — Spring Boot HTTP adapter (port 8080)
       |
       v
MCPServer — Authoritative game engine (port 9090)
       |
       v
PostgreSQL (port 5432)
```

**Core principle**: MCPServer owns all game logic. MCPClient is a thin HTTP-to-MCP adapter. The frontend talks only to MCPClient.

## What the Game Does

Players manage an air base, assigning aircraft to missions and managing fuel, weapons, maintenance, and spare parts across rounds. The goal is to complete all assigned missions in as few rounds as possible. The game ends in **WON** (all missions complete) or **LOST** (no operational aircraft).

**Round flow**: Planning → Mission assignment → Dice rolls (damage) → Landing decisions → Round complete (deliveries applied)

## Frontend (`/smartairbase`)

- React 19, single large component (`App.js`, ~3500 lines)
- Four tabs:
  - **Play** — interactive game with manual or auto-play modes
  - **Simulator** — batch game execution with aggregate results
  - **Scenario Editor** — customize base/aircraft/supply configs
  - **Dashboard** — analytics on finished games, CSV export
- Polling-based (no WebSockets); all actions are HTTP requests
- Color coding in the `Play` tab:
  - green — completed mission cards
  - blue — healthy aircraft on base slots
  - orange — aircraft needing repair on base slots
  - red — destroyed aircraft
- An always-visible color legend on the right side of the `Play` tab explains the active mapping

## MCPClient (`/MCPClient`)

- Spring Boot 4 + Spring AI 2.0
- REST controller (`GameController`) exposes `/api` endpoints
- `McpToolExecutor` serializes requests and calls MCP tools over SSE
- `AutoPlayService` handles automated mission assignment and landing decisions
- `AnalysisFeedService` generates round commentary (rule-based or LLM)
- `RolePromptFactory` builds phase-aware system and user prompts for each of the four narration personas
- `GET /api/games/{gameId}/summary` returns a `GameSummaryResponseDTO` combining the analytics snapshot and the final per-role narration for a finished game
- Pluggable LLM via Maven profiles:
  - `local` — Ollama
  - `cloud` — OpenAI gpt-4o-mini
  - `cloud-gemini` — Google Gemini 2.0 Flash

## MCPServer (`/MCPServer`)

- Spring Boot 4 + Spring AI MCP Server + PostgreSQL
- MCP tools are `@Tool`-annotated Spring methods:
  - `GameTools` — game lifecycle, scenarios, analytics, simulations
  - `RoundTools` — round phases (start, dice rolls, landings, complete)
  - `MissionTools` — mission assignment
  - `AircraftTools` / `BaseTools` — state reads
- 15 Liquibase migrations manage schema evolution
- `SimulationBatchService` runs background batch games
- `GameAnalyticsSnapshotService` denormalizes finished-game data for dashboard queries

## Key Game Mechanics

### Dice Outcomes

| Dice Roll | Outcome          | Repair Rounds |
|-----------|------------------|---------------|
| 1         | Destroyed        | —             |
| 2         | Full service     | 4             |
| 3         | Major repair     | 3             |
| 4         | Component damage | 2             |
| 5         | Minor repair     | 1             |
| 6         | No fault         | 0             |

### Supply Deliveries

- Fuel: every 2 rounds
- Weapons: every 4 rounds
- Spare parts: every 3 rounds

### Mission Costs

| Mission | Fuel | Weapons | Flight Hours |
|---------|------|---------|--------------|
| M1 Recon      | 20 | 0 | 4 |
| M2 Strike     | 30 | 2 | 6 |
| M3 Deep Strike| 40 | 4 | 8 |

## Notable Patterns

- **MCP as RPC** — Spring AI wraps tool calls; responses unwrapped into typed DTOs
- **State machine phases** — Round and aircraft statuses control valid actions
- **Scenario versioning** — SCN_STANDARD is read-only; users duplicate and edit copies
- **Dual narration** — Analysis feed is either rule-based or LLM-generated (configurable via `smartairbase.analysis.narration-mode`)
- **Denormalized analytics** — Post-game snapshot table enables fast dashboard queries
- **Post-game summary** — When a game ends, the `Play` tab shows a summary panel with a win/loss badge, aggregate stats, final per-role narration from all four personas, and a collapsible round-by-round replay of the analysis feed

## Running Locally

Start services in this order:

1. PostgreSQL
2. `MCPServer`
3. `MCPClient`
4. `smartairbase`

### PostgreSQL

```bash
cd MCPServer
docker-compose up -d
```

### MCPServer

```bash
cd MCPServer
./mvnw spring-boot:run
```

### MCPClient

```bash
cd MCPClient
./mvnw spring-boot:run -Plocal
```

Replace `-Plocal` with `-Pcloud` or `-Pcloud-gemini` to use a different LLM provider.

### Frontend

```bash
cd smartairbase
npm start
```

Open:

- Frontend: `http://localhost:3000`
- Client API: `http://localhost:8080`
- MCP server SSE: `http://localhost:9090/sse`

## Notes

- `MCPServer` is the source of truth for all rules and state transitions.
- `MCPClient` unwraps MCP tool responses and returns typed DTO-based HTTP responses.
- Aborting a game marks it as `ABORTED` in the backend; a new game must be created to continue playing.
- Analysis feed entries show whether text came from `LLM` or `Rule-based` narration, and are persisted in PostgreSQL.
- The `Scenario editor` tab is disabled while a game is active to keep scenario editing and live play separate.
- Analytics snapshots are written for every finished game (from both Play and Simulator) and include scenario, win/loss, rounds, dice profile, aircraft counts, mission mix, and resource totals.
- Each narration persona uses phase-aware prompts: round commentary uses round facts, and end-of-game narration uses a separate final prompt with the full game outcome.
- The four analysis personas are: `Captain Erik Holm (Pilot)`, `Sara Lind (Ground Crew Chief)`, `Johan Berg (Lead Maintenance Technician)`, and `Colonel Anna Sjöberg (Command / Operations)`.
