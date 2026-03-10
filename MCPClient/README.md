# MCPClient

Spring Boot client for Smart Air Base that uses an external MCP server as the authoritative game engine.

The client is responsible for:
- invoking MCP tools through Spring AI MCP Client
- exposing a simple HTTP API for the browser
- serving a lightweight web UI for semi-automated gameplay
- showing a local rule reference for Smart Air Base v7
- making automatic planning and landing decisions on top of the server state

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
The MCP server remains authoritative for legality checks. The client adds an autopilot layer that chooses missions and landing bases.

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
- [src/main/java/se/smartairbase/mcpclient/service/AutoPlayService.java](/Users/ahmedhb/development/hackit/hackit_202603/saabsmartairbase/smartairbase/MCPClient/src/main/java/se/smartairbase/mcpclient/service/AutoPlayService.java)
  Implements automatic mission assignment, automatic landing decisions, and automatic round completion when possible.

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
- `POST /api/games/{gameId}/rounds/next`
- `POST /api/games/{gameId}/dice-rolls/auto`
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

The current player-facing flow is intentionally simplified. A player only needs to:

1. create a game
2. start the next round
3. submit dice rolls for aircraft that completed missions
4. repeat until the game is won or lost

The client is designed around the server's phase-based round flow, but the autopilot endpoints hide the manual planning and landing steps.

### 1. Create a Game

Create a new game before any other action:

- `POST /api/games`

After creation, keep the returned `gameId`.

### 2. Start the Next Round

Start a new round through the autopilot endpoint:

- `POST /api/games/{gameId}/rounds/next`

This endpoint will automatically:

- start the round
- inspect the current game state
- choose aircraft for available missions
- assign those missions
- resolve the planning phase
- return the updated state together with the next expected player action

If no aircraft are sent on missions and the round can be closed immediately, the client may also complete the round automatically.

### 3. Submit Dice Rolls

For each aircraft waiting for damage resolution:

- `POST /api/games/{gameId}/dice-rolls/auto`

This endpoint will automatically:

- record the player's dice result
- detect when the round enters the landing phase
- choose landing bases automatically for all aircraft awaiting landing
- send aircraft to holding if no legal landing exists
- complete the round automatically when all pending decisions are resolved
- return the updated state and the next expected player action

### 4. Inspect State When Needed

Fetch the current game state whenever the UI or operator needs an updated view:

- `GET /api/games/{gameId}`

Optional detail endpoints:

- `GET /api/games/{gameId}/aircraft/{aircraftCode}`
- `GET /api/games/{gameId}/bases/{baseCode}`

### 5. Repeat Until Game End

If the game is still active:

1. start the next round:
   `POST /api/games/{gameId}/rounds/next`
2. submit dice rolls through:
   `POST /api/games/{gameId}/dice-rolls/auto`

### Typical Round Sequence

```text
create game
-> start next round
-> system assigns missions automatically
-> player records dice roll for each pending aircraft
-> system chooses landing bases automatically
-> system completes round automatically when possible
-> repeat until win/loss
```

## Autopilot Strategy

The client uses the current game state together with the local v7 rules reference to make decisions.

Mission assignment:

- considers only `READY` aircraft and `AVAILABLE` missions
- rejects aircraft that cannot satisfy fuel, weapons, or remaining flight hour requirements
- searches combinations of aircraft-to-mission assignments
- prefers combinations that maximize completed mission count first
- then prefers higher-value missions
- then prefers lower resource waste to preserve stronger aircraft for future rounds

Landing selection:

- queries the server for legal landing options
- prioritizes damaged aircraft with the most constrained repair needs first
- prefers bases that can start maintenance immediately when damage exists
- preserves high-capability bases when simpler bases are sufficient
- prefers bases with weapons and fuel if remaining missions still require them
- sends aircraft to holding only when the server reports that no legal base can accept them

The MCP server still validates every action. The autopilot is a client-side decision layer, not a replacement for server-side rules.

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
- automatic planning and landing flow in `AutoPlayService`
- rule reference data in `GameRulesReferenceService`
- controller delegation and request validation in `GameController`

## Current Limitations

- The client currently uses raw `JsonNode` responses in the MCP layer instead of fully typed response DTOs.
- The local rule reference is documentation for the UI, not an executable rule engine.
- The autopilot uses deterministic heuristics based on the current scenario and state; it is not a generic solver for arbitrary future scenarios.
- The web client is intended as a lightweight operator UI, not a full production game frontend.
