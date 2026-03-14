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
- abort the current game and return the UI to its initial state
- run non-visualized simulator batches and inspect aggregate results
- inspect finished-game analytics in a dashboard and export filtered rows to CSV

The player does not manually assign missions or landing bases in the main flow. Those decisions are made by `MCPClient` autoplay.

## Main Files

- `src/App.js`
  Main screen, API integration, create-game form, abort-game behavior, and round flow.
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
- optional game name, chosen through a prompt after pressing `Create game`
- aircraft count
- mission count for each mission type
- max rounds

The scenario version is kept internally and is no longer shown in the GUI.
The scenario name is selected from a dropdown and is not free-text editable.
Default scenario name: `SCN_STANDARD`.
If the user does not provide a custom name, the backend generates a default game name such as `GAME_001`.
If the user provides a custom game name, it must be unique. Duplicate names are rejected and shown as an error in the UI.
`Max rounds` in `Play` is a hard upper bound on how many rounds a run may take. If the limit is reached without a win, the game ends as a loss because it took too many rounds.
`Create game` is disabled while an active game is loaded, so the operator cannot start a second live game from the same screen state.
The aircraft field is capped at `8` and the UI explains that this is the maximum for the current scenario.
Under `Game ID`, the control panel also shows a read-only `Current game name` field that always reflects the loaded game or `No active game`.
The `Scenario editor` tab is also disabled while an active game is running, so scenario editing only happens from a non-live control state.

### Scenario Rules Panel

Under the scenario selector the UI includes a toggle button that shows a compact rules panel for the selected scenario.

The panel now summarizes the currently selected scenario from real scenario data and can reflect user-edited scenario copies. It includes:

- aircraft start values
- mission costs
- total parking and maintenance capacity
- delivery schedules for fuel, weapons, and spare parts
- dice outcome meanings
- the fact that some rounds may be pure wait rounds

### Scenario Editor

For editable custom scenarios, the scenario editor can now change:

- base parking and repair capacity
- base `fuel`, `weapons`, and `spare parts` start/max values
- delivery amounts for existing supply rules
- initial aircraft count per aircraft type by changing the actual scenario aircraft list

The scenario editor does not add new bases or delivery rules, and delivery frequency remains read-only reference data.
`SCN_STANDARD` remains read-only and cannot be edited directly.

Additional fixed rules in the editor:

- `Base C` stays a fuel outpost in every custom scenario
- only `Fuel` values are editable for `Base C`
- `Base C` repair slots remain locked to `0`
- `Base C` weapons and spare-parts stocks remain locked to `0`
- `Base C` weapons and spare-parts delivery amounts remain locked to `0`
- initial aircraft count cannot go below `1`
- initial aircraft count cannot exceed the sum of all base parking slots in the scenario

Saved custom-scenario values are not only cosmetic. When a new game is created from that scenario, the game uses:

- the edited base capacities
- the edited base start/max inventories
- the edited delivery amounts
- the edited scenario aircraft list

The frontend now creates those games from the selected `scenarioId`, not just by scenario name, so the exact custom scenario copy currently shown in the editor is the one materialized into live game state.
When an edited scenario replaces its aircraft rows, the backend flushes the old rows before inserting the updated list so reused aircraft codes such as `F1` remain valid.

### Simulator

The `Simulator` tab starts a backend batch instead of playing a visible match.

The simulator form exposes:

- unique batch name
- scenario
- run count
- aircraft count
- mission mix
- dice strategy
- max rounds per run

While a batch is running:

- `Play` is locked
- `Scenario editor` is locked
- the UI shows progress, elapsed time, and aggregate result cards

Simulator results are aggregated from the server-side analytics snapshot rows created by each finished run.

### Dashboard

The `Dashboard` tab is a browser view over the persisted `game_analytics_snapshot` dataset.

It currently supports:

- newest runs first
- filters with explicit `All` values for:
  - scenario
  - run date
  - aircraft count
  - `M1`
  - `M2`
  - `M3`
- paging with `20` rows per page
- `Page X of Y` status text
- total filtered row count
- separate `Run date` and `Run time` columns
- CSV export of all currently filtered rows

The export flow uses:

- an operator-provided file name
- semicolon-separated values (`;`)
- the browser save dialog where `showSaveFilePicker` is supported
- a normal download fallback otherwise

### Abort Game

The `Abort game` button ends the currently active game instead of only clearing the screen.

When the user clicks `Abort game`:

- the frontend stops automation timers
- the frontend aborts in-flight HTTP requests
- the UI calls `POST /api/games/{gameId}/abort`
- the backend marks the game as `ABORTED`
- the current `Game ID` is cleared from the UI
- control-panel state is restored to its defaults
- the visible event history and analysis feed are cleared from the UI

`Abort game` is disabled when no active game exists and becomes available only after a game has been created and loaded.

This means the aborted game cannot be continued from the current session. The user must create a new game to keep playing.

Abort does not delete stored backend history. Persisted analysis feed entries and other game data remain attached to the aborted game.

### Manual and Automated Round Flow

Automated mode:

- `Next turn` keeps the one-click autoplay flow
- mission preview, dice rolls, and next-round progression each have separate wait settings
- the dice strategy selector supports:
  - `Random dice outcome` for `1..6`
  - `Favor as little damage as possible` for `4`, `5`, `6`
  - `Cause as much damage as possible` for `1`, `2`, `3`

Manual mode:

- `Next turn` starts the round and prepares mission assignments
- `Resolve missions` moves those aircraft from `On mission` into `Awaiting dice roll`
- `Roll dice` stays manual

The `Play` tab is also laid out to match that flow more closely:

- `Control center` appears before `Missions`
- `Mission complete` appears under `On mission` and before `Awaiting dice roll`
- `Dice Outcome` appears after `Awaiting dice roll`
- `Event history` and `Analysis feed` are shown below the bases section, stacked vertically at the same content width as the bases area
- an always-visible right-side `Aircraft colors` legend explains the active color mapping used in the `Play` tab

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
- base cards with slot layout, maximum inventory values, and delivery timing summaries for the currently selected scenario before game start
- aircraft cards in `Holding`, `Park`, and `Repair` with `current/max` values for fuel, weapons, and flight hours
- positive `Added:` diffs on aircraft cards when fuel, weapons, or flight hours have been restored since the previous state refresh
- flight hours should only increase after full service, not after ordinary landing or ordinary repair

Aircraft cards in `Park` and `Repair` are now compact by default. They show the aircraft ID immediately, while the fuller status text and value details appear on hover.
The compact hover tooltip uses a black border and a dark steel-blue background to match the current visual theme.
Color usage in `Play` currently follows:

- completed missions: green mission cards
- aircraft that need repair on base slots: orange slot cards
- healthy aircraft on base slots: blue slot cards
- destroyed aircraft: red cards

### Event History

The event history panel now shows:

- event title
- timestamp
- round number when available
- event-specific details such as dice outcome meaning and create-game messages

### Dice Selection Analytics

The frontend now tags each dice-roll request with a `diceSelectionMode` so later statistics can distinguish:

- manual direct dice choice
- manual random dice choice
- automated random dice choice
- automated min-damage dice choice
- automated max-damage dice choice

`MCPServer` persists the exact mode on each roll and derives a game-level `diceSelectionProfile` from the full set of recorded rolls.

Every finished game also writes one analytics row on the server with setup features and final outcome data such as:

- rounds to outcome
- completed mission count
- surviving aircraft count
- destroyed aircraft count
- aggregate base and delivery values

Those persisted rows are what both the `Dashboard` tab and the simulator result summaries read from.

### Analysis Feed

The UI includes an `Analysis feed` panel that behaves like a running commentary stream.

- entries are shown in chronological order, with the latest entry at the bottom
- the panel auto-scrolls to the newest entry
- each entry shows the narration source as `LLM` or `Rule-based`
- each entry is written by a named persona:
  - `Captain Erik Holm (Pilot)`
  - `Sara Lind (Ground Crew Chief)`
  - `Johan Berg (Lead Maintenance Technician)`
  - `Colonel Anna Sjöberg (Command / Operations)`

The feed is populated by `MCPClient` after round analysis has been generated.
The saved feed history is persisted by `MCPServer`, so it survives browser refreshes and `MCPClient` restarts.

## API Endpoints Used

- `GET /api/reference/rules`
- `GET /api/analytics/games`
- `POST /api/games`
- `GET /api/games/{gameId}`
- `POST /api/games/{gameId}/abort`
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
- `src/App.test.js` now covers the locked `Scenario editor` tab, the active/inactive `Create game` and `Abort game` buttons, and the read-only current game name field in the control panel.
- This is still a dashboard-style UI, not a final polished game client.
