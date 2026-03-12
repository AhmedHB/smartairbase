# MCPClient

`MCPClient` is a Spring Boot adapter between the browser-facing HTTP API and the authoritative `MCPServer`.

It is responsible for:

- calling MCP tools over SSE through Spring AI
- exposing a typed HTTP API under `/api`
- unwrapping MCP tool content responses into DTOs
- running autoplay logic for mission assignment and landings
- generating the round-based analysis feed shown in the frontend
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
- `POST /api/games/{gameId}/abort`
- `GET /api/games/{gameId}/analysis-feed`
- `POST /api/games/{gameId}/analysis/generate`
- `POST /api/games/{gameId}/rounds/next`
- `POST /api/games/{gameId}/rounds/plan`
- `POST /api/games/{gameId}/dice-rolls/auto`
- `POST /api/games/{gameId}/rounds/start`
- `POST /api/games/{gameId}/missions/assign`
- `POST /api/games/{gameId}/missions/resolve`
- `POST /api/games/{gameId}/missions/resolve-auto`
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
- `gameName` (optional)
- `aircraftCount`
- `missionTypeCounts`

Example:

```json
{
  "scenarioName": "SCN_STANDARD",
  "gameName": "GAME_001",
  "aircraftCount": 5,
  "missionTypeCounts": {
    "M1": 3,
    "M2": 2,
    "M3": 1
  }
}
```

If `gameName` is omitted, `MCPServer` generates a default such as `GAME_001`.

## Autoplay Behavior

`POST /api/games/{gameId}/rounds/next`:

- starts the round
- assigns missions automatically
- resolves mission execution
- may complete the round immediately if nothing is pending
- supports rounds where no action is possible and the game effectively waits for the next round

`POST /api/games/{gameId}/rounds/plan`:

- starts the round
- assigns missions automatically
- stops in `PLANNING` so the frontend can show aircraft in `On mission` before mission resolution

`POST /api/games/{gameId}/missions/resolve-auto`:

- resolves the already planned missions
- moves the round into dice, landing, or direct completion
- returns the same autoplay-style response shape used by the frontend

`POST /api/games/{gameId}/dice-rolls/auto`:

- records one dice roll
- resolves all landing choices automatically when the round enters `LANDING`
- completes the round automatically when legal
- returns current state instead of failing the UI if a late dice request arrives after the round already moved to `LANDING`

The returned game state reflects the post-landing or post-round server state, which means aircraft may already have been refueled or rearmed by the time the browser renders the response.

`POST /api/games/{gameId}/abort`:

- aborts the active game
- marks the game as `ABORTED` in `MCPServer`
- makes the game unavailable for continued play
- allows the frontend to clear its active state and require a new game creation

Flight hours are different from fuel and weapons:

- they are not restored on ordinary landing
- they are not restored by ordinary repair
- they are restored only when actual full service completes, either because the dice outcome required full service or because the aircraft had reached `0` remaining flight hours

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

Analysis narration mode:

```yaml
smartairbase:
  analysis:
    # valid values: hybrid, rule-based, llm
    narration-mode: hybrid
```

- `hybrid`: try LLM narration first and fall back to rule-based text
- `rule-based`: always use the deterministic rule-based analysis text
- `llm`: require LLM narration for analysis entries

## Running Locally

Prerequisites:

- Java 21
- `MCPServer` running on `9090`
- Ollama installed locally if analysis narration is configured to use a local Ollama model
- Ollama model `qwen2.5:7b` installed locally

Install the required Ollama model:

```bash
ollama pull qwen2.5:7b
```

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
- Scenario name normalization accepts old `smartairbase` input as an alias for `SCN_STANDARD`.
- `GameRulesReferenceService` provides the English scenario summary and key numbers shown in the frontend rules panel, including deliveries, holding fuel cost, capacity, and dice outcomes.
- The client currently drives a UI that shows aircraft `current/max` values and positive `Added:` diffs based on successive game-state snapshots.
- Base lookups in autoplay normalize both `A` and `BASE_A` style codes so landing logic stays aligned with runtime state and rules reference data.
- The analysis feed uses named personas per role:
  - `Captain Erik Holm (Pilot)`
  - `Sara Lind (Ground Crew Chief)`
  - `Johan Berg (Lead Maintenance Technician)`
  - `Colonel Anna Sjöberg (Command / Operations)`
- Each analysis feed item exposes a narration source so the frontend can label it as `LLM` or `Rule-based`.
- `MCPClient` generates narration text, but the saved analysis feed is now persisted through `MCPServer` MCP tools so the history survives client restarts.
- When the frontend aborts a game, it clears the visible analysis feed in the browser, but persisted feed history remains stored for that game on the server side.
- The current round-diff snapshot used to shape the next narration remains client-local and is not yet persisted.
- The frontend create flow can now ask the operator to choose between a generated default game name and a custom game name before `POST /api/games` is sent.
