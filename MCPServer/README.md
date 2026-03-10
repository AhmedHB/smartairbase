# MCPServer

`MCPServer` is the authoritative Smart Air Base game engine. It exposes game actions as MCP tools over SSE and persists game state in PostgreSQL.

It owns:

- game state
- scenario materialization
- round progression
- mission resolution
- damage and landing flow
- maintenance and supply updates
- win/loss evaluation

## Architecture

```text
MCP Client / Agent
        |
        v
Spring AI MCP Server
        |
        v
Service Layer
        |
        v
PostgreSQL
```

## MCP Tools

### Game

- `create_game(scenarioName, version, aircraftCount, missionTypeCounts)`
- `get_game_state(gameId)`

### Mission

- `assign_mission(gameId, aircraftCode, missionCode)`

### Round

- `start_round(gameId)`
- `resolve_missions(gameId)`
- `record_dice_roll(gameId, aircraftCode, diceValue)`
- `list_available_landing_bases(gameId, aircraftCode)`
- `land_aircraft(gameId, aircraftCode, baseCode)`
- `send_aircraft_to_holding(gameId, aircraftCode)`
- `complete_round(gameId)`

### Detail Reads

- `get_aircraft_state(gameId, aircraftCode)`
- `get_base_state(gameId, baseCode)`

## Dynamic Game Creation

Games are built from the seeded `SmartAirBase V7` scenario template.

At creation time the current implementation can override:

- total aircraft count
- mission count per mission type

Runtime generation rules:

- aircraft codes: `F1`, `F2`, `F3`, ...
- mission codes: `M1-1`, `M1-2`, `M2-1`, ...

The server still uses seeded base types, repair rules, and resource settings from the scenario.

## Round Model

Round phases:

- `PLANNING`
- `DICE_ROLL`
- `LANDING`
- `ROUND_COMPLETE`

Typical sequence:

1. `start_round`
2. `assign_mission`
3. `resolve_missions`
4. `record_dice_roll`
5. `land_aircraft` or `send_aircraft_to_holding`
6. `complete_round`

## Important State

Aircraft statuses:

- `READY`
- `ON_MISSION`
- `AWAITING_DICE_ROLL`
- `AWAITING_LANDING`
- `WAITING_MAINTENANCE`
- `IN_MAINTENANCE`
- `HOLDING`
- `CRASHED`

Implemented outcome rules:

- `WON` when all missions are completed
- `LOST` when no operational aircraft remain

## Persistence

Liquibase manages schema and seed data.

Important files:

- `src/main/resources/db/changelog/001-create-schema.yml`
- `src/main/resources/db/changelog/002-load-data.yml`
- `src/main/resources/db/changelog/003-create-game-schema.yml`

Seed data includes:

- aircraft types
- base types and capabilities
- mission types
- repair rules
- `SmartAirBase V7`

## Running Locally

Start PostgreSQL:

```bash
docker-compose up -d
```

Run the server:

```bash
./mvnw spring-boot:run
```

Default ports:

- app: `9090`
- SSE endpoint: `http://localhost:9090/sse`

Datasource config lives in:

- `src/main/resources/application.yaml`

## Build and Test

```bash
./mvnw clean package
./mvnw test
```

If Liquibase checksums got out of sync in a reused local database:

```bash
./mvnw test -Dspring.liquibase.clear-checksums=true -Dspring.liquibase.drop-first=true
```

## Notes

- Read services use Spring read-only transactions to avoid lazy-proxy failures while mapping DTOs.
- The latest stepwise round implementation notes are in `docs/CHANGELOG_2026-03-10_stepwise_round.md`.
