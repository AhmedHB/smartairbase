# MCPClient

`MCPClient` is a Spring Boot adapter between the browser-facing HTTP API and the authoritative `MCPServer`.

It is responsible for:

- calling MCP tools over SSE through Spring AI
- exposing a typed HTTP API under `/api`
- unwrapping MCP tool content responses into DTOs
- running autoplay logic for mission assignment and landings
- serving a lightweight built-in UI at `/`

## Role in the System

```text
Frontend
  |
  v
GameController
  |
  v
SmartAirBaseMcpClient
  |
  v
McpToolExecutor
  |
  v
MCPServer
```

`MCPClient` is not the rule engine. `MCPServer` still validates every action.

## Main Components

- `controller/GameController.java`
  Public HTTP API.
- `service/SmartAirBaseMcpClient.java`
  Typed facade over MCP tools.
- `service/McpToolExecutor.java`
  Serializes requests, calls MCP tools, unwraps `text` payloads, and deserializes typed responses.
- `service/AutoPlayService.java`
  Chooses mission assignments and landing decisions.
- `service/GameRulesReferenceService.java`
  Serves local rule-reference data used by the UI.

## HTTP API

Main endpoints:

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

## Create Game Request

`POST /api/games` supports:

- `scenarioName`
- `version`
- `aircraftCount`
- `missionTypeCounts`

Example:

```json
{
  "scenarioName": "SCN_STANDARD",
  "version": "7",
  "aircraftCount": 5,
  "missionTypeCounts": {
    "M1": 3,
    "M2": 2,
    "M3": 1
  }
}
```

## Autoplay Behavior

`POST /api/games/{gameId}/rounds/next`:

- starts the round
- assigns missions automatically
- resolves mission execution
- may complete the round immediately if nothing is pending
- supports rounds where no action is possible and the game effectively waits for the next round

`POST /api/games/{gameId}/dice-rolls/auto`:

- records one dice roll
- resolves all landing choices automatically when the round enters `LANDING`
- completes the round automatically when legal

The returned game state reflects the post-landing or post-round server state, which means aircraft may already have been refueled or rearmed by the time the browser renders the response.

Autoplay prioritizes:

- completing as many missions as possible
- higher-value missions
- lower resource waste
- landing damaged aircraft where maintenance can start quickly

## Configuration

Default ports:

- app: `8080`
- actuator: `8081`

Default MCP server connection:

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

Main config files:

- `src/main/resources/application.yml`
- `src/main/resources/application-local.yml`
- `src/main/resources/application-cloud.yml`

## Running Locally

Prerequisites:

- Java 21
- `MCPServer` running on `9090`

Run:

```bash
./mvnw spring-boot:run -Plocal
```

Then open:

```text
http://localhost:8080
```

## Build and Test

```bash
./mvnw clean package
./mvnw test
```

## Notes

- The client API now returns typed DTO responses, not raw MCP envelopes.
- Scenario/version normalization accepts old `smartairbase` input as an alias for `SCN_STANDARD`, and `7` as an alias for `V7`.
- `GameRulesReferenceService` provides the English scenario summary and key numbers shown in the frontend rules panel, including deliveries, holding fuel cost, capacity, and dice outcomes.
- The client currently drives a UI that shows aircraft `current/max` values and positive `Added:` diffs based on successive game-state snapshots.
