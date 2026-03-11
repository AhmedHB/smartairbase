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

## Gameplay Model

Each game tracks:

- aircraft
- bases
- missions
- current round
- active round phase
- win/loss status

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

Rounds with no available actions are also valid. If mission resolution leaves no aircraft waiting for dice or landing, the round can move directly to completion and the player simply waits for the next round.

## Dynamic Game Creation

Games are created from the seeded `SCN_STANDARD V7` scenario template, but the current implementation also supports custom setup at creation time:

- number of aircraft
- number of missions per mission type (`M1`, `M2`, `M3`)

The frontend exposes these options in the create-game form.
The scenario is selected from a dropdown and currently supports `SCN_STANDARD`.
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

## Win and Loss

Current implemented outcome rules:

- `WON` when all missions are completed
- `LOST` when no operational aircraft remain

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
- The frontend `Reset` button creates a fresh game from the current create-game settings.
- The frontend includes a scenario rules panel in English with mission costs, deliveries, holding fuel cost, total capacity, and dice outcomes for `SCN_STANDARD`.
