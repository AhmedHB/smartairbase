# Smart Air Base

A turn-based strategy simulation about **mission planning, aircraft readiness, base logistics, and constrained resource management**.

Smart Air Base models how a limited fleet of aircraft can complete missions while dealing with fuel consumption, weapon usage, maintenance capacity, spare parts, parking limits, and the risk of holding or crashing when no base can receive an aircraft.

The game is designed to work well for:

- AI planning and autonomous decision-making
- deterministic simulation
- operational logistics experimentation
- formal modeling, including Petri-net inspired analysis

---

## Overview

The player manages a small air operation consisting of:

- **Aircraft** that can fly missions and consume resources
- **Bases** with different capabilities and capacity limits
- **Missions** with different fuel, weapon, and flight-time costs
- **Rounds** that drive mission execution, maintenance, and periodic deliveries

The objective is to complete all missions in as few rounds as possible while keeping as many aircraft alive and operational as possible.

---

## Game Objective

### Win condition

You win when:

- all missions are completed

### Lose conditions

You lose when:

- all aircraft are destroyed
- the remaining missions can no longer be completed

---

## Core Gameplay Loop

Each round follows a fixed sequence:

```text
START ROUND
   |
   v
+-------------------+
| 1. Planning       |
+-------------------+
   |
   v
+-------------------+
| 2. Resource Check |
+-------------------+
   |
   v
+-------------------+
| 3. Mission Phase  |
+-------------------+
   |
   v
+-------------------+
| 4. Damage Roll    |
+-------------------+
   |
   v
+-----------------------------+
| 5. Landing / Holding        |
|    Maintenance / Waiting    |
+-----------------------------+
   |
   v
+-------------------+
| 6. Deliveries     |
+-------------------+
   |
   v
+-------------------+
| Win / Lose Check  |
+-------------------+
   |
   +----> End Game
   |
   +----> Next Round
```

---

## System Architecture Diagram

The project can be understood as a game platform with a rules engine at the center.

```text
+--------------------+        +--------------------+
|      Frontend      |        |    AI / MCP Client |
|  UI / Visualization|        | Planning / Control |
+----------+---------+        +----------+---------+
           |                             |
           +-------------+---------------+
                         |
                         v
              +------------------------+
              |     Game / MCP API     |
              |   Application Layer    |
              +-----------+------------+
                          |
                          v
              +------------------------+
              |     Rules Engine       |
              |  Round + Mission Logic |
              |  Validation + State    |
              +-----------+------------+
                          |
            +-------------+-------------+
            |             |             |
            v             v             v
   +----------------+ +----------------+ +----------------+
   | Aircraft Model | |   Base Model   | | Mission Model  |
   +----------------+ +----------------+ +----------------+
            \             |             /
             \            |            /
              +-----------+-----------+
                          |
                          v
              +------------------------+
              | Persistence / Storage  |
              | Config / DB / Saves    |
              +------------------------+
```

### Architecture roles

- **Frontend** shows the current game state and lets a player inspect rounds, bases, aircraft, and missions.
- **AI / MCP Client** can plan actions and invoke deterministic tools.
- **Game / MCP API** exposes the game state and actions.
- **Rules Engine** is the authoritative place where all rule validation and state transitions happen.
- **Domain models** represent Aircraft, Bases, and Missions.
- **Persistence / Storage** keeps scenario data, saved game state, or configuration.

---

## Domain Model Diagram

This simplified domain model shows the most important game entities and how they relate to each other.

```text
+------------------------------------------------------+
|                       Game                           |
|------------------------------------------------------|
| - round                                              |
| - status                                             |
| - missionsRemaining                                  |
+----------------------+-------------------------------+
                       |
      +----------------+-------------------+
      |                                    |
      v                                    v
+-------------+                    +---------------+
|  Aircraft   |                    |     Base      |
|-------------|                    |---------------|
| id          |                    | id            |
| name        |                    | name          |
| status      |                    | type          |
| fuel        |                    | parkingSlots  |
| weapons     |                    | maintSlots    |
| flightHours |                    | fuelStock     |
| damageState |                    | weaponStock   |
| location    |                    | spareParts    |
+------+------+                    +-------+-------+
       |                                   ^
       | assigned / lands / repaired at    |
       |                                   |
       v                                   |
+-------------+                            |
|  Mission    |----------------------------+
|-------------|   launched from / resolved via base support
| id          |
| name        |
| fuelCost    |
| weaponCost  |
| flightTime  |
| status      |
+-------------+
```

### Main domain relationships

- A **Game** contains the current round state, all aircraft, all bases, and all missions.
- An **Aircraft** is located at a base, in maintenance, in holding, on a mission, or destroyed.
- A **Base** stores resources and provides capabilities such as refuel, rearm, repair, or full service.
- A **Mission** consumes aircraft time and resources when executed.

---

## Starting Scenario

The standard scenario starts with:

- **3 aircraft**
- **3 bases**: A, B, C
- **3 missions**
- all aircraft starting at **Base A**

### Aircraft initial capacity

- Fuel: **100**
- Weapons: **6**
- Flight hours before full service: **20**

### Initial aircraft state

```text
F1 – Base A – fuel 100 – weapons 6 – flight hours 20
F2 – Base A – fuel 100 – weapons 6 – flight hours 20
F3 – Base A – fuel 100 – weapons 6 – flight hours 20
```

---

## Missions

The baseline mission set contains three mission types:

| Mission | Name        | Flight Time | Fuel Cost | Weapon Cost |
|--------:|-------------|------------:|----------:|------------:|
| M1      | Recon       | 4           | 20        | 0           |
| M2      | Strike      | 6           | 30        | 2           |
| M3      | Deep Strike | 8           | 40        | 4           |

### Mission rules

- A mission can only be assigned to an aircraft that is available.
- The aircraft must have enough **fuel**, **weapons**, and remaining **flight hours**.
- After mission completion:
  - fuel is reduced
  - weapons are reduced
  - remaining flight hours are reduced

---

## Bases

Each base has parking capacity, maintenance capacity, resource storage, and operational capabilities.

### Base A — Main Airbase

- Parking slots: **4**
- Maintenance slots: **2**

**Starting storage**
- Fuel: **300**
- Weapons: **20**
- Spare parts: **10**

**Max storage**
- Fuel: **500**
- Weapons: **40**
- Spare parts: **20**

**Capabilities**
- Refuel
- Rearm
- Repair
- Full service

### Base B — Forward Base

- Parking slots: **2**
- Maintenance slots: **1**

**Starting storage**
- Fuel: **200**
- Weapons: **10**
- Spare parts: **4**

**Max storage**
- Fuel: **300**
- Weapons: **20**
- Spare parts: **10**

**Capabilities**
- Refuel
- Rearm
- Light repair

### Base C — Fuel Outpost

- Parking slots: **2**
- Maintenance slots: **0**

**Starting storage**
- Fuel: **150**
- Weapons: **0**
- Spare parts: **0**

**Max storage**
- Fuel: **200**
- Weapons: **0**
- Spare parts: **0**

**Capabilities**
- Refuel only

---

## Aircraft Rules

Each aircraft has a limited operational capacity.

### Aircraft limits

- Maximum fuel: **100**
- Maximum weapons: **6**
- Maximum flight hours before mandatory full service: **20**

### Aircraft states

An aircraft may be in one of several logical states:

- Ready
- On mission
- Parked
- In maintenance
- Waiting for maintenance
- Holding
- Crashed
- Destroyed

---

## Fuel Rules

Fuel is consumed when flying missions.

### Refuel

An aircraft may refuel after landing if:

- the base supports **refuel**
- the base has fuel in stock

The aircraft is refilled up to maximum capacity or until the base runs out of fuel.

### Fuel delivery

Fuel is delivered every **second round**:

| Base | Delivery |
|------|---------:|
| A    | +50      |
| B    | +40      |
| C    | +30      |

A base may never exceed its maximum fuel storage.

---

## Weapon Rules

Weapons are consumed by combat missions.

### Rearm

An aircraft may rearm after landing if:

- the base supports **rearm**
- the base has weapons in stock

The aircraft is refilled up to maximum capacity or until the base runs out of weapons.

### Weapon delivery

Weapons are delivered every **fourth round**:

| Base | Delivery |
|------|---------:|
| A    | +6       |
| B    | +4       |
| C    | +0       |

A base may never exceed its maximum weapon storage.

---

## Spare Parts and Repair Rules

Spare parts are needed for repairs and full service.

### Spare parts delivery

Spare parts are delivered every **third round**:

| Base | Delivery |
|------|---------:|
| A    | +3       |
| B    | +2       |
| C    | +0       |

A base may never exceed its maximum spare-parts storage.

### Spare parts required by repair type

| Repair type        | Spare parts required |
|--------------------|---------------------:|
| Minor repair       | 1                    |
| Component damage   | 2                    |
| Major repair       | 3                    |
| Full service       | 4                    |

---

## Damage and Maintenance

After each mission, the aircraft that flew must roll for damage.

### Damage table

| Dice roll | Result              |
|----------:|---------------------|
| 1         | No damage           |
| 2         | Minor repair        |
| 3         | Minor repair        |
| 4         | Component damage    |
| 5         | Major repair        |
| 6         | Full service needed |

### Repair duration

| Dice roll / result | Repair time |
|--------------------|------------:|
| 1 / No damage      | 0 rounds    |
| 2 / Minor repair   | 1 round     |
| 3 / Minor repair   | 1 round     |
| 4 / Component dmg  | 2 rounds    |
| 5 / Major repair   | 3 rounds    |
| 6 / Full service   | 4 rounds    |

### Mandatory full service

When an aircraft’s remaining flight hours reach **0**, it must complete **full service** before it may fly another mission.

---

## Landing, Parking, and Maintenance Slots

When an aircraft lands at a base, it must occupy a **parking slot**.

### Rules

- If there is a free parking slot, the aircraft may land.
- **Maintenance slots** are only used when repairs are actively being performed.
- If parking is available but maintenance is full, the aircraft may wait parked at the base.
- If no base can receive the aircraft, it enters **holding**.

---

## Holding and Crash Rules

Holding models an aircraft circling in the air because no base can accept it.

### Holding fuel cost

- **5 fuel per round**

### Crash conditions

- If fuel reaches **0**, the aircraft crashes.
- If fuel is less than the amount needed to continue holding and no base can accept the aircraft, the aircraft crashes.

---

## Decision Points

Version 7 introduces several important operational decision points:

- Can the aircraft fly the chosen mission?
- Does it have enough fuel, weapons, and flight hours?
- Can it land at any base after the mission?
- Is there free parking?
- Is there a free maintenance slot?
- Does the base have enough spare parts?
- Can the base refuel or rearm the aircraft?
- Will the aircraft remain in holding too long and crash?
- Are all missions completed, or has the game become unwinnable?

---

## Example Full Round Logic

A typical round can be interpreted like this:

1. **Planning**
   - choose which aircraft will fly
   - choose which missions they will perform

2. **Resource check**
   - verify fuel requirement
   - verify weapon requirement
   - verify remaining flight hours
   - verify aircraft availability

3. **Mission execution**
   - aircraft flies
   - mission is marked complete
   - aircraft resources are reduced

4. **Damage roll**
   - roll 1–6 for each aircraft that flew
   - determine maintenance requirement

5. **Landing and maintenance**
   - try to land
   - occupy parking if possible
   - enter maintenance if required and possible
   - otherwise wait or enter holding

6. **Deliveries**
   - apply fuel / spare parts / weapon deliveries according to round number

7. **Win / lose check**
   - stop if all missions are completed
   - stop if no further mission completion is possible

---

## Why This Game Is Interesting

Smart Air Base is not only about assigning missions. It is about balancing:

- short-term mission success
- long-term aircraft survival
- base congestion
- repair bottlenecks
- periodic resource shortages
- logistics planning under constrained capacity

This makes the game useful as:

- a strategy prototype
- a logistics simulation
- an AI planning benchmark
- a formal modeling case study

---

## Suggested Repository Structure

```text
smart-air-base/
├── README.md
├── docs/
│   ├── rules/
│   ├── diagrams/
│   └── examples/
├── game-engine/
├── mcp-server/
├── frontend/
└── scenarios/
```

---

## Documentation Sources

This README is based on the current rules specification and process overview used in the project.

- `SmartAirBase_Regler_v7.txt`
- `Overview.txt`

---

## License

Add your project license here.
