# MCPServer

![Java](https://img.shields.io/badge/Java-21+-blue) ![Spring
Boot](https://img.shields.io/badge/Spring_Boot-4.0.3-brightgreen)
![Maven](https://img.shields.io/badge/build-Maven-red)
![Docker](https://img.shields.io/badge/Docker-required-blue)
![npm](https://img.shields.io/badge/npm-required-orange)

------------------------------------------------------------------------

# 🚀 Overview

**MCPServer** is the backend service for the **Smart Air Base** strategy
game.

The server exposes game functionality through **MCP tools**, allowing
MCP-compatible clients such as AI agents, development tools, or custom
frontends to interact with the game world.

MCPServer is responsible for:

-   Managing game state
-   Enforcing game rules
-   Executing missions and actions
-   Persisting game data

All persistent data is stored in **PostgreSQL**, while **Liquibase**
manages schema evolution and seed data.

The server exposes its functionality through **MCP tools over
Server-Sent Events (SSE)** using **Spring AI MCP Server**.

The backend acts as the **authoritative game engine**, meaning all rule
validation and state transitions occur on the server.

------------------------------------------------------------------------

# 🏗 System Architecture

    Client / AI Agent
            │
            ▼
    MCP Tool Invocation (SSE)
            │
            ▼
    MCPServer (Spring Boot + Spring AI MCP)
            │
            ▼
    Service Layer (Game Logic)
            │
            ▼
    PostgreSQL Database

------------------------------------------------------------------------

# 📦 Project Structure

    MCPServer/
    ├── src/main/java/           Application source code
    ├── src/main/resources/      Configuration & Liquibase changelogs
    ├── docs/                    Documentation
    ├── docker-compose.yml       PostgreSQL container setup
    ├── pom.xml                  Maven configuration
    └── README.md

------------------------------------------------------------------------

# 🧠 MCP Tools

MCP tools represent **deterministic operations** within the game.\
They can be invoked by MCP clients to interact with the game world.

Each tool:

-   performs a specific operation
-   validates input
-   executes business logic
-   returns structured JSON

Tools are grouped by domain.

------------------------------------------------------------------------

# 🎮 Game Tools

### createGame

Creates a new game instance.

Typical use:

-   initialize a game
-   reset the system state

Example request:

``` json
{
  "tool": "createGame"
}
```

Example response:

``` json
{
  "gameId": 1,
  "status": "CREATED",
  "round": 1
}
```

------------------------------------------------------------------------

### getGame

Returns information about the current game state.

Example:

``` json
{
  "tool": "getGame",
  "gameId": 1
}
```

Response:

``` json
{
  "gameId": 1,
  "round": 3,
  "status": "RUNNING"
}
```

------------------------------------------------------------------------

# 🛩 Aircraft Tools

### listAircraft

Returns all aircraft in the game.

Example request:

``` json
{
  "tool": "listAircraft"
}
```

Response:

``` json
[
  {
    "id": 1,
    "name": "F-16",
    "status": "AVAILABLE"
  }
]
```

------------------------------------------------------------------------

### getAircraft

Returns detailed information about a specific aircraft.

Example:

``` json
{
  "tool": "getAircraft",
  "aircraftId": 1
}
```

------------------------------------------------------------------------

# 🏠 Base Tools

### listBases

Returns all air bases.

Example:

``` json
{
  "tool": "listBases"
}
```

------------------------------------------------------------------------

### getBase

Returns detailed information about a specific base.

Example:

``` json
{
  "tool": "getBase",
  "baseId": 2
}
```

Response:

``` json
{
  "id": 2,
  "name": "Northern Air Base",
  "capacity": 10
}
```

------------------------------------------------------------------------

# 🎯 Mission Tools

### listMissions

Returns available missions.

Example:

``` json
{
  "tool": "listMissions"
}
```

------------------------------------------------------------------------

### assignMission

Assigns an aircraft to a mission.

Example:

``` json
{
  "tool": "assignMission",
  "aircraftId": 3,
  "missionId": 7
}
```

------------------------------------------------------------------------

# 🔄 Round Tools

### startRound

Starts a new round in the simulation.

Example:

``` json
{
  "tool": "startRound"
}
```

------------------------------------------------------------------------

### endRound

Ends the current round and processes results.

Example:

``` json
{
  "tool": "endRound"
}
```

------------------------------------------------------------------------

# 🔁 Example Game Flow

Typical interaction flow:

1.  Client connects to SSE endpoint
2.  Client lists available MCP tools
3.  Client calls `createGame`
4.  Client queries bases and aircraft
5.  Client assigns missions
6.  Client advances the simulation with `startRound` and `endRound`

Sequence example:

    Client → MCPServer : createGame
    Client → MCPServer : listAircraft
    Client → MCPServer : assignMission
    Client → MCPServer : startRound
    Client → MCPServer : endRound

------------------------------------------------------------------------

# ⚙️ Running the Server

## Prerequisites

Install:

-   Java 21
-   Maven
-   Docker
-   npm

------------------------------------------------------------------------

## Start PostgreSQL

    docker-compose up -d

------------------------------------------------------------------------

## Start MCPServer

    mvn spring-boot:run

Server runs on:

    http://localhost:9090

SSE endpoint:

    http://localhost:9090/sse

------------------------------------------------------------------------

# 🔎 MCP Inspector

To inspect available MCP tools:

    npx @modelcontextprotocol/inspector

Then connect to:

    http://localhost:9090/sse

Inspector allows you to:

-   list tools
-   manually call tools
-   inspect responses

------------------------------------------------------------------------

# 🔐 Security Notes

Database credentials are currently stored in:

    application.yaml

For production environments:

-   move secrets to environment variables
-   never commit credentials
-   use secure configuration management

------------------------------------------------------------------------

# 📌 Summary

MCPServer provides:

-   A Spring Boot MCP backend
-   Deterministic MCP tools for gameplay operations
-   PostgreSQL persistence
-   Liquibase-managed schema evolution
-   SSE-based tool communication
-   Integration with MCP-compatible AI agents

The server acts as the **central game engine** for the Smart Air Base
system.

------------------------------------------------------------------------

# 📄 License

Specify license here.
