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
- submit dice rolls
- inspect bases, aircraft, and mission progress
- reset into a fresh game using the current configuration

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

### Reset

The `Reset` button creates a completely new game using the current create-game settings.

### Mission Cards

Mission cards are shown dynamically:

- before game creation from the configured mission counts
- after game creation from the actual runtime mission list returned by the backend

Runtime mission codes can therefore look like:

- `M1-1`
- `M1-2`
- `M2-1`

## API Endpoints Used

- `GET /api/reference/rules`
- `POST /api/games`
- `GET /api/games/{gameId}`
- `POST /api/games/{gameId}/rounds/next`
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
