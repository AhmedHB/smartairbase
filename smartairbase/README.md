# Smart Air Base Frontend

This is the React frontend for Smart Air Base.

It talks only to `MCPClient`, never directly to `MCPServer`.

## Role in the System

```text
React Frontend
    |
    v
MCPClient HTTP API
    |
    v
MCPServer
```

## What the UI Does

The frontend provides a guided operator workflow:

- create a game
- choose number of aircraft
- choose number of missions per mission type
- start the next round
- in manual mode, resolve planned missions explicitly
- submit dice rolls
- inspect bases, aircraft, and mission progress
- reset the UI back to its initial state

The player does not manually assign missions or landing bases in the main flow. Those decisions are made by `MCPClient` autoplay.

## Main Files

- `src/App.js`
  Main screen, API integration, create-game form, reset behavior, and round flow.
- `src/App.css`
  Dashboard styling.
- `src/index.css`
  Global styles.
- `src/App.test.js`
  Basic rendering and mission-card tests.

## Current UI Behavior

### Create Game Form

The create-game form currently exposes:

- scenario name
- aircraft count
- mission count for each mission type

The scenario version is kept internally and is no longer shown in the GUI.
The scenario name is selected from a dropdown and is not free-text editable.
Default scenario name: `SCN_STANDARD`.
The aircraft field is capped at `8` and the UI explains that this is the maximum for the current scenario.

### Scenario Rules Panel

Under the scenario selector the UI includes a toggle button that shows a compact rules panel for the selected scenario.

For `SCN_STANDARD` the panel currently summarizes:

- aircraft start values
- mission costs
- total parking and maintenance capacity
- holding fuel cost
- delivery schedules for fuel, weapons, and spare parts
- dice outcome meanings
- the fact that some rounds may be pure wait rounds
- the current holding crash rule after fuel has already reached `0`

### Reset

The `Reset` button does not create a new game anymore. It resets the UI, stops automation, clears the active game from the screen, restores default control-panel settings, and writes a reset entry to event history.

### Manual and Automated Round Flow

Automated mode:

- `Next turn` keeps the one-click autoplay flow
- mission preview, dice rolls, and next-round progression each have separate wait settings

Manual mode:

- `Next turn` starts the round and prepares mission assignments
- `Resolve missions` moves those aircraft from `On mission` into `Awaiting dice roll`
- `Roll dice` stays manual

### Mission Cards

Mission cards are shown dynamically:

- before game creation from the configured mission counts
- after game creation from the actual runtime mission list returned by the backend

Runtime mission codes can therefore look like:

- `M1-1`
- `M1-2`
- `M2-1`

### Holding and Bases

The main board also includes:

- `On mission`, `Awaiting dice roll`, `Holding`, and `Destroyed aircraft` panels ordered to match the round flow
- a dedicated `Holding` panel for aircraft that could not land
- a dedicated `Destroyed aircraft` panel for aircraft that have crashed or been lost
- support text under `Missions`, `Holding`, and `Bases`
- base cards with maximum inventory values and delivery timing summaries for the current scenario
- aircraft cards in `Holding`, `Park`, and `Repair` with `current/max` values for fuel, weapons, and flight hours
- positive `Added:` diffs on aircraft cards when fuel, weapons, or flight hours have been restored since the previous state refresh
- flight hours should only increase after full service, not after ordinary landing or ordinary repair

## API Endpoints Used

- `GET /api/reference/rules`
- `POST /api/games`
- `GET /api/games/{gameId}`
- `POST /api/games/{gameId}/rounds/next`
- `POST /api/games/{gameId}/rounds/plan`
- `POST /api/games/{gameId}/missions/resolve-auto`
- `POST /api/games/{gameId}/dice-rolls/auto`

## Environment

Environment variable:

- `REACT_APP_API_BASE_URL`

Default:

```text
http://localhost:8080/api
```

Example:

```bash
REACT_APP_API_BASE_URL=http://localhost:8080/api npm start
```

## Running Locally

Prerequisites:

- Node.js
- npm
- `MCPClient` running locally
- `MCPServer` reachable through `MCPClient`

Run:

```bash
npm start
```

Open:

```text
http://localhost:3000
```

## Build and Test

Build:

```bash
npm run build
```

Test:

```bash
npm test -- --watchAll=false
```

## Notes

- The frontend expects clean DTO-shaped JSON from `MCPClient`.
- State refresh is action-driven; there is no live push from the backend.
- This is still a dashboard-style UI, not a final polished game client.
