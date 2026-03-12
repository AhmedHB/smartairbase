# Smart Air Base

Smart Air Base is a turn-based air-operations game about missions, aircraft readiness, base capacity, repairs, fuel, weapons, spare parts, and constrained landings.

The repository contains three applications:

- `MCPServer`
  The authoritative game engine and persistence layer.
- `MCPClient`
  A Spring Boot HTTP API that talks to `MCPServer` over MCP/SSE and adds autoplay decisions.
- `smartairbase`
  A React frontend that talks to `MCPClient`.

## Current Architecture

```text
React Frontend (:3000)
        |
        v
MCPClient HTTP API (:8080)
        |
        v
Spring AI MCP Client over SSE
        |
        v
MCPServer (:9090)
        |
        v
PostgreSQL (:5432)
```

## Game Rules

### Objective

The player runs an air base under resource and capacity constraints.
The goal is to complete all missions in as few rounds as possible without ending up in a state where no remaining mission can be flown.

### Core State

Each game tracks:

- aircraft
- bases
- missions
- current round
- active round phase
- win/loss status

### Scenario Setup

The default scenario is `SCN_STANDARD`.

Initial setup:

- 3 bases: `A`, `B`, `C`
- 3 aircraft at start: `F1`, `F2`, `F3`
- each aircraft starts with:
  - `Fuel 100`
  - `Weapons 6`
  - `Flight Hours 20`

Base summary:

- Base `A`: main airbase with the highest parking, maintenance, fuel, weapon, and spare-parts capacity
- Base `B`: forward base with limited parking and maintenance, supports lighter service than `A`
- Base `C`: fuel outpost with parking but no repair or rearm capability

### Missions

Mission types in the standard scenario:

- `M1 Recon`: `20` fuel, `0` weapons, `4` flight hours
- `M2 Strike`: `30` fuel, `2` weapons, `6` flight hours
- `M3 Deep Strike`: `40` fuel, `4` weapons, `8` flight hours

Mission instances are created dynamically with runtime codes such as:

- `M1-1`
- `M1-2`
- `M2-1`

### Round Flow

Round phases:

- `PLANNING`
- `DICE_ROLL`
- `LANDING`
- `ROUND_COMPLETE`

Typical round sequence:

1. start round
2. assign missions
3. resolve missions
4. record dice rolls
5. land aircraft or send them to holding
6. complete round

UI flow:

- automated mode keeps a one-click round start and then auto-runs dice and landings
- manual mode is split into `Next turn` and `Resolve missions` so the player can inspect the `On mission` state before dice handling starts

Dice strategy options in the frontend:

- `Random dice outcome` uses the full range `1..6`
- `Favor as little damage as possible` only uses `4`, `5`, `6`
- `Cause as much damage as possible` only uses `1`, `2`, `3`

Rounds with no available actions are valid. If mission resolution leaves no aircraft waiting for dice or landing, the round can move directly to completion.

### Mission Resolution

When a mission is resolved:

- mission cost is applied immediately to the aircraft
- fuel decreases
- weapons decrease
- remaining flight hours decrease
- the aircraft leaves its base and moves to `AWAITING_DICE_ROLL`

### Dice Outcomes

After a mission, each flown aircraft receives a dice outcome:

- `1` = `Destroyed`
- `2` = `Full service required`
- `3` = `Major repair`
- `4` = `Component damage`
- `5` = `Minor repair`
- `6` = `No fault`

Repair duration:

- `1 Destroyed` -> `0` rounds
- `2 Full service required` -> `4` rounds
- `3 Major repair` -> `3` rounds
- `4 Component damage` -> `2` rounds
- `5 Minor repair` -> `1` round
- `6 No fault` -> `0` rounds

`Destroyed` is terminal:

- the aircraft goes directly to `DESTROYED`
- it does not land
- it does not enter maintenance
- it does not consume spare parts

### Landing and Base Services

After dice resolution, aircraft that survived must land at a base.

Landing rules:

- a base must have a free parking slot
- a damaged aircraft may only land at a base that supports the required service level
- if no base can accept the aircraft, it must go to `HOLDING`

Ground service and maintenance rules:

- after landing, a base may refuel and rearm the aircraft immediately if the base supports those services and has stock
- repair and full service consume maintenance capacity and spare parts
- aircraft with no damage become `READY` after landing
- damaged aircraft either enter maintenance immediately or wait for maintenance capacity/resources

Flight-hour rule:

- flight hours are never restored on ordinary landing
- ordinary repair does not restore flight hours
- flight hours are restored only when actual full service completes

If an aircraft reaches `0` remaining flight hours, it must undergo full service before it can be used again.

### Holding

Holding rule in the current implementation:

- aircraft in `HOLDING` lose `5` fuel at round completion
- when fuel first reaches `0`, the aircraft remains in holding
- if it is still in holding at a later round completion with `0` fuel, it crashes

### Deliveries and Resource Pressure

The standard scenario includes recurring deliveries:

- fuel deliveries every `2` rounds
- spare-parts deliveries every `3` rounds
- weapon deliveries every `4` rounds

This means some rounds are pure waiting rounds where the correct move is to advance until resources or maintenance capacity become available again.

### Win and Loss

Implemented outcome rules:

- `WON` when all missions are completed and all surviving aircraft are back in `READY` state at a base
- `LOST` when no operational aircraft remain

Destroyed aircraft do not block victory by themselves as long as all missions are completed and all surviving aircraft have been recovered to a stable end state.

### Example Play

Example start:

- `F1`, `F2`, `F3` start at base `A`
- all aircraft start with `Fuel 100`, `Weapons 6`, `Flight Hours 20`
- missions available: `M1`, `M2`, `M3`

Round 1:

- assign `F1 -> M1`
- assign `F2 -> M2`
- `F3` waits
- after mission resolution:
  - `F1` becomes `Fuel 80`, `Weapons 6`, `Flight Hours 16`
  - `F2` becomes `Fuel 70`, `Weapons 4`, `Flight Hours 14`
- dice:
  - `F1 = 6` -> `No fault`
  - `F2 = 4` -> `Component damage`
- result:
  - `F1` lands at base `A` and becomes `READY`
  - `F2` lands at base `A` and starts maintenance
  - `F2` consumes `2` spare parts and needs `2` repair rounds

Round 2:

- only `M3` remains
- assign `F1 -> M3`
- `F2` continues maintenance
- `F3` waits
- after mission resolution:
  - `F1` becomes `Fuel 40`, `Weapons 2`, `Flight Hours 8`
- dice:
  - `F1 = 2` -> `Full service required`
- result:
  - `F1` lands at base `A` and enters full service
  - `F1` consumes `4` spare parts and needs `4` rounds
  - round-end deliveries still apply normally

This example shows three different post-mission outcomes:

- normal return with no fault
- ordinary repair after component damage
- long full-service downtime after a severe dice result

## Dynamic Game Creation

Games are created from the seeded `SCN_STANDARD` scenario template, but the current implementation also supports custom setup at creation time:

- game name
- number of aircraft
- number of missions per mission type (`M1`, `M2`, `M3`)

The frontend exposes these options in the create-game form.
The scenario is selected from a dropdown and currently supports `SCN_STANDARD`.
After pressing `Create game`, the UI asks whether to use a generated default game name or provide a custom one.
If no custom name is provided, the backend assigns a generated default such as `GAME_001`, `GAME_002`, and so on.
If a custom game name is provided, it must be unique. Reusing an existing game name returns a validation error.
While a game is active, the `Create game` button is disabled so the operator must either finish or abort that game first.
The control panel also shows both the current `Game ID` and a read-only `Current game name` field.
While a game is active, the `Scenario editor` tab is also disabled so the operator cannot switch into scenario editing mid-session.
The aircraft field is capped at `8`, which matches the total parking capacity in the current scenario.

Mission instances are generated with runtime codes such as:

- `M1-1`
- `M1-2`
- `M2-1`

Aircraft are generated as:

- `F1`
- `F2`
- `F3`
- ...

## Running Locally

Start services in this order:

1. PostgreSQL
2. `MCPServer`
3. `MCPClient`
4. `smartairbase`

### PostgreSQL

Use the compose file in `MCPServer`:

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

### Frontend

```bash
cd smartairbase
npm start
```

Open:

- frontend: `http://localhost:3000`
- client API: `http://localhost:8080`
- MCP server SSE: `http://localhost:9090/sse`

## Notes

- `MCPServer` is the source of truth for rules and state transitions.
- `MCPClient` unwraps MCP tool responses and returns typed DTO-based HTTP responses.
- The frontend `Abort game` button now aborts the active game, stops automation, clears the active game from the screen, and returns the UI to its initial state.
- Aborting a game marks it as `ABORTED` in the backend, so that game cannot be continued and a new game must be created to keep playing.
- The `Abort game` button is disabled when no active game exists, and becomes available only for a running game.
- The `Scenario editor` tab is disabled for the same active-game window, keeping scenario editing and live play separate in the UI.
- The frontend also clears the visible event log and analysis feed when a game is aborted.
- Persisted backend history is not deleted by abort. Analysis feed items and other game history remain stored for the aborted game.
- The frontend includes a scenario rules panel in English that is now generated from the currently selected scenario data, while keeping the same compact title/summary/point-list format.
- The frontend now shows the round flow as `On mission`, `Awaiting dice roll`, `Holding`, and `Destroyed aircraft`, plus per-aircraft `current/max` stats and positive `Added:` diffs for fuel, weapons, and flight hours.
- Before a game has started, the play board previews base slot layout and base resource limits from the currently selected scenario.
- Event history entries now show both timestamp and round number when that information is available.
- Automated mode has separate wait settings for mission preview, dice rolls, and next-round progression.
- The frontend also includes an `Analysis feed` panel backed by `MCPClient`, where named personas comment on each round as a running feed:
  - `Captain Erik Holm (Pilot)`
  - `Sara Lind (Ground Crew Chief)`
  - `Johan Berg (Lead Maintenance Technician)`
  - `Colonel Anna Sjöberg (Command / Operations)`
- Each analysis entry shows whether the text came from `LLM` or `Rule-based` narration.
- `MCPClient` supports `smartairbase.analysis.narration-mode` with `hybrid`, `rule-based`, and `llm`.
- Analysis entries are now persisted by `MCPServer` in the same PostgreSQL database as the game state, while `MCPClient` still generates the narration text.
