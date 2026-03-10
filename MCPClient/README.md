# MCPClient

Spring Boot client for Smart Air Base that uses an external MCP server as the authoritative game engine.

The client is responsible for:
- invoking MCP tools through Spring AI MCP Client
- exposing a simple HTTP API for the browser
- serving a lightweight web UI for step-by-step gameplay
- showing a local rule reference for Smart Air Base v7

The server is responsible for:
- game logic
- rule validation
- round progression
- persistence

## Overview

High-level architecture:

```text
Browser UI
   |
   v
GameController (/api + static assets)
   |
   v
SmartAirBaseMcpClient
   |
   v
McpToolExecutor
   |
   v
Spring AI MCP Client over SSE
   |
   v
MCPServer
```

The client does not duplicate runtime game rules. It forwards player actions to the MCP server and renders the resulting state and outcomes.

## Main Components

Core classes:

- [src/main/java/se/smartairbase/mcpclient/controller/GameController.java](/Users/ahmedhb/development/hackit/hackit_202603/saabsmartairbase/smartairbase/MCPClient/src/main/java/se/smartairbase/mcpclient/controller/GameController.java)
  HTTP API for creating games, controlling rounds, recording dice rolls, landing aircraft, and fetching state.
- [src/main/java/se/smartairbase/mcpclient/service/SmartAirBaseMcpClient.java](/Users/ahmedhb/development/hackit/hackit_202603/saabsmartairbase/smartairbase/MCPClient/src/main/java/se/smartairbase/mcpclient/service/SmartAirBaseMcpClient.java)
  Facade that maps client request objects to MCP tool calls.
- [src/main/java/se/smartairbase/mcpclient/service/McpToolExecutor.java](/Users/ahmedhb/development/hackit/hackit_202603/saabsmartairbase/smartairbase/MCPClient/src/main/java/se/smartairbase/mcpclient/service/McpToolExecutor.java)
  Resolves the correct `ToolCallback`, serializes the request to JSON, and parses the response as `JsonNode`.
- [src/main/java/se/smartairbase/mcpclient/service/GameRulesReferenceService.java](/Users/ahmedhb/development/hackit/hackit_202603/saabsmartairbase/smartairbase/MCPClient/src/main/java/se/smartairbase/mcpclient/service/GameRulesReferenceService.java)
  Exposes local reference data from rule version 7 to the UI.

Static UI assets:

- [src/main/resources/static/index.html](/Users/ahmedhb/development/hackit/hackit_202603/saabsmartairbase/smartairbase/MCPClient/src/main/resources/static/index.html)
- [src/main/resources/static/app.js](/Users/ahmedhb/development/hackit/hackit_202603/saabsmartairbase/smartairbase/MCPClient/src/main/resources/static/app.js)
- [src/main/resources/static/styles.css](/Users/ahmedhb/development/hackit/hackit_202603/saabsmartairbase/smartairbase/MCPClient/src/main/resources/static/styles.css)

## MCP Tools Used by the Client

The client invokes the following tools on the MCP server:

- `create_game`
- `get_game_state`
- `assign_mission`
- `start_round`
- `resolve_missions`
- `record_dice_roll`
- `list_available_landing_bases`
- `land_aircraft`
- `send_aircraft_to_holding`
- `complete_round`
- `get_aircraft_state`
- `get_base_state`

## Client HTTP API

The client exposes the following endpoints under `/api`:

- `GET /api/reference/rules`
- `POST /api/games`
- `GET /api/games/{gameId}`
- `POST /api/games/{gameId}/rounds/start`
- `POST /api/games/{gameId}/missions/assign`
- `POST /api/games/{gameId}/missions/resolve`
- `POST /api/games/{gameId}/dice-rolls`
- `GET /api/games/{gameId}/landing-bases?aircraftCode=...`
- `POST /api/games/{gameId}/landings`
- `POST /api/games/{gameId}/holding?aircraftCode=...`
- `POST /api/games/{gameId}/rounds/complete`
- `GET /api/games/{gameId}/aircraft/{aircraftCode}`
- `GET /api/games/{gameId}/bases/{baseCode}`

The browser UI is available at `/`.

## Recommended API Order

The client is designed around the server's phase-based round flow. The APIs should normally be used in this order:

### 1. Create a Game

Create a new game before any other action:

- `POST /api/games`

After creation, keep the returned `gameId`.

### 2. Inspect the Current State

Fetch the current game state whenever the UI or operator needs an updated view:

- `GET /api/games/{gameId}`

Optional detail endpoints:

- `GET /api/games/{gameId}/aircraft/{aircraftCode}`
- `GET /api/games/{gameId}/bases/{baseCode}`

### 3. Start a Round

Open a new round:

- `POST /api/games/{gameId}/rounds/start`

### 4. Assign Missions During Planning

Assign zero or more missions while the round is in the planning phase:

- `POST /api/games/{gameId}/missions/assign`

When planning is finished:

- `POST /api/games/{gameId}/missions/resolve`

### 5. Record Dice Rolls

For each aircraft waiting for damage resolution:

- `POST /api/games/{gameId}/dice-rolls`

The server moves the round forward as aircraft are resolved.

### 6. Resolve Landing Decisions

For each aircraft waiting to land:

1. Check available landing bases:
   `GET /api/games/{gameId}/landing-bases?aircraftCode=...`
2. If a valid base is available:
   `POST /api/games/{gameId}/landings`
3. If no base can accept the aircraft:
   `POST /api/games/{gameId}/holding?aircraftCode=...`

### 7. Complete the Round

When all dice and landing decisions are resolved:

- `POST /api/games/{gameId}/rounds/complete`

This applies end-of-round effects such as maintenance progression, holding handling, deliveries, and win/loss checks.

### 8. Repeat Until Game End

If the game is still active:

1. Fetch state if needed:
   `GET /api/games/{gameId}`
2. Start the next round:
   `POST /api/games/{gameId}/rounds/start`

### Typical Round Sequence

```text
create game
-> get game state
-> start round
-> assign mission (0..n times)
-> resolve missions
-> record dice roll (for each pending aircraft)
-> list landing bases
-> land aircraft or send to holding
-> complete round
-> repeat until win/loss
```

## Configuration

Main configuration files:

- [src/main/resources/application.yml](/Users/ahmedhb/development/hackit/hackit_202603/saabsmartairbase/smartairbase/MCPClient/src/main/resources/application.yml)
- [src/main/resources/application-local.yml](/Users/ahmedhb/development/hackit/hackit_202603/saabsmartairbase/smartairbase/MCPClient/src/main/resources/application-local.yml)
- [src/main/resources/application-cloud.yml](/Users/ahmedhb/development/hackit/hackit_202603/saabsmartairbase/smartairbase/MCPClient/src/main/resources/application-cloud.yml)

Current default MCP server connection:

```yaml
spring:
  ai:
    mcp:
      client:
        sse:
          connections:
            intelliplan:
              url: http://localhost:9090/sse
```

Default runtime ports:

- client: `http://localhost:8080`
- actuator: `http://localhost:8081`
- MCP server SSE: `http://localhost:9090/sse`

## Running Locally

Prerequisites:

- Java 21
- Maven Wrapper (`./mvnw`)
- the MCP server running locally
- for the `local` profile: Ollama available if that part of the config is used
- for the `cloud` profile: `OPENAI_API_KEY`

Start the client:

```bash
./mvnw spring-boot:run -Plocal
```

Or:

```bash
./mvnw spring-boot:run -Pcloud
```

Then open:

```text
http://localhost:8080
```

## Build and Test

Build the project:

```bash
./mvnw clean package
```

Run tests:

```bash
./mvnw test
```

Current test coverage includes:

- payload mapping in `SmartAirBaseMcpClient`
- tool resolution in `McpToolExecutor`
- rule reference data in `GameRulesReferenceService`
- controller delegation and request validation in `GameController`

## Current Limitations

- The client currently uses raw `JsonNode` responses in the MCP layer instead of fully typed response DTOs.
- The local rule reference is documentation for the UI, not an executable rule engine.
- The web client is intended as a lightweight operator UI, not a full production game frontend.
