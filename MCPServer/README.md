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
- simulator batch execution
- analytics snapshot persistence for finished games
- analytics dataset queries for the dashboard

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

- `create_game(scenarioName, version, gameName, aircraftCount, missionTypeCounts, maxRounds)`
- `create_game_from_scenario(scenarioId, gameName, aircraftCount, missionTypeCounts, maxRounds)`
- `get_game_state(gameId)`
- `abort_game(gameId)`
- `list_analysis_feed(gameId)`
- `append_analysis_feed_items(gameId, items)`

### Simulation

- `create_simulation_batch(batchName, scenarioName, aircraftCount, missionTypeCounts, diceStrategy, runCount, maxRounds)`
- `get_simulation_batch(simulationBatchId)`

### Analytics

- `list_game_analytics_snapshots(scenarioName, createdDate, aircraftCount, m1Count, m2Count, m3Count)`

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

Dice analytics metadata:

- each persisted `dice_roll` row now stores `diceSelectionMode`
- each `game` row stores a derived `diceSelectionProfile`

This allows later statistics to distinguish exact roll-selection behavior per throw while still classifying the overall game as, for example, `AUTO_MIN_DAMAGE`, `MANUAL_MIXED`, or `MIXED`.

## Dynamic Game Creation

Games are built from the seeded `SCN_STANDARD` scenario template.

For generic creation flows, `create_game(...)` still resolves a scenario by name/version.
For editable custom scenarios, the UI now uses `create_game_from_scenario(...)` so the exact selected `scenarioId` is materialized into runtime game tables without re-looking it up by name.
This is the path that preserves scenario-editor changes such as base capacities, start inventories, delivery amounts, and the edited scenario aircraft list.

When a scenario update replaces the aircraft rows for one scenario, the service flushes the deletes before inserting the replacement rows. That keeps reused aircraft codes such as `F1` valid under the unique `(scenario_id, code)` constraint.

At creation time the current implementation can override:

- game name
- total aircraft count
- mission count per mission type

Runtime generation rules:

- game names default to `GAME_001`, `GAME_002`, ... when no explicit name is supplied
- explicit custom game names must be unique across all persisted games
- the optional `maxRounds` limit is stored per game and forces a loss if the game stays active beyond that round budget
- frontend operators are expected to create at most one active game at a time from the main control panel
- aircraft codes: `F1`, `F2`, `F3`, ...
- mission codes: `M1-1`, `M1-2`, `M2-1`, ...

The server still uses seeded base types, repair rules, and resource settings from the scenario.

For editable custom scenarios, game creation now also materializes:

- edited base parking and maintenance capacity
- edited base start/max stocks
- edited delivery amounts for existing scenario supply rules
- the edited scenario aircraft list, including changed initial aircraft count

Invariant rules still apply:

- system scenarios remain read-only
- Base `C` remains fuel-only
- Base `C` repair capacity is always `0`
- Base `C` weapons and spare-parts stocks are always `0`
- Base `C` weapons and spare-parts delivery amounts are always `0`
- scenario aircraft count must stay between `1` and the total parking capacity across all scenario bases

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

The server-side phase model itself is unchanged by the newer manual UI flow. The split between `Next turn` and `Resolve missions` is implemented in `MCPClient` and the frontend by calling these same MCP tools in two separate steps.

If mission resolution produces no aircraft in `AWAITING_DICE_ROLL` and no landing decisions are pending, the server now transitions directly to `LANDING`, allowing an immediate round completion. This supports valid "wait only" rounds.

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

- `WON` when all missions are completed and all surviving aircraft are back in `READY` state at a base
- `LOST` when no operational aircraft remain
- `ABORTED` when a client explicitly aborts the current game

Holding behavior:

- aircraft in `HOLDING` lose `5` fuel at round completion
- an aircraft that reaches `0` fuel may still remain in holding for that round
- if it is still in holding at a later round completion with `0` fuel, it crashes

Ground service behavior:

- mission fuel, weapons, and flight-hour cost are applied to the aircraft during mission resolution
- landing may immediately trigger `REFUEL` and `REARM` from the base stock
- repair/full service consume spare parts and maintenance slots
- flight hours are never restored on ordinary landing
- ordinary repair does not restore flight hours
- flight hours are restored only when actual `FULL_SERVICE_REQUIRED` work completes, whether that requirement came from the dice outcome or from the aircraft reaching `0` remaining flight hours

## Persistence

Liquibase manages schema and seed data.

Important files:

- `src/main/resources/db/changelog/001-create-schema.yml`
- `src/main/resources/db/changelog/002-load-data.yml`
- `src/main/resources/db/changelog/009-create-simulation-batch-schema.yml`
- `src/main/resources/db/changelog/011-create-game-analytics-snapshot-schema.yml`

For a fresh database, the baseline changelog chain is now intentionally short:

- `001` creates the core runtime schema, including `game_analysis_entry`
- `002` loads seed data and fixes seed sequences
- `009` creates simulator batch tables
- `011` creates the analytics snapshot table

This means deleting the local database and restarting `MCPServer` recreates the final schema directly instead of replaying a longer chain of historical add-column migrations.

Seed data includes:

- aircraft types
- base types and capabilities
- mission types
- repair rules
- `SCN_STANDARD`
- simulator batches in `simulation_batch` and `simulation_batch_game`
- one analytics dataset row per finished game in `game_analytics_snapshot`

`game_analytics_snapshot` is also the source for the dashboard view in the frontend. The dashboard does not rebuild analytics from event history; it reads these persisted rows directly through `MCPClient`.

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
- `canCompleteRound` is only exposed when the active round is in `LANDING`, which keeps autoplay aligned with the server-side phase model.
- The latest stepwise round implementation notes are in `docs/CHANGELOG_2026-03-10_stepwise_round.md`.
- Round analysis narration is not generated in `MCPServer`; that feed is built in `MCPClient` on top of server state and event transitions.
- `MCPServer` now persists analysis feed entries in PostgreSQL via `game_analysis_entry`, keyed by game, round, and role so the history belongs to the game and survives client restarts.
- Aborting a game does not delete its persisted history. The game becomes inactive, but stored analysis feed entries and other saved state remain in PostgreSQL for audit and debugging.
