import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { within } from '@testing-library/react';
import App, { automatedDiceSelectionMode, manualDiceSelectionMode } from './App';

beforeEach(() => {
  let currentGameName = 'test';
  let analyticsRows = [
    {
      gameAnalyticsSnapshotId: 2,
      gameId: 12,
      gameName: 'GAME_002',
      scenarioName: 'WINTER_OPS',
      gameStatus: 'LOST',
      isWin: false,
      roundsToOutcome: 4,
      diceSelectionProfile: 'AUTO_MAX_DAMAGE',
      aircraftCount: 2,
      survivingAircraftCount: 1,
      destroyedAircraftCount: 1,
      missionCount: 2,
      completedMissionCount: 1,
      m1Count: 1,
      m2Count: 0,
      m3Count: 1,
      createdAt: '2026-03-13T10:00:00',
    },
    {
      gameAnalyticsSnapshotId: 1,
      gameId: 11,
      gameName: 'GAME_001',
      scenarioName: 'SCN_STANDARD',
      gameStatus: 'WON',
      isWin: true,
      roundsToOutcome: 2,
      diceSelectionProfile: 'AUTO_RANDOM',
      aircraftCount: 3,
      survivingAircraftCount: 3,
      destroyedAircraftCount: 0,
      missionCount: 3,
      completedMissionCount: 3,
      m1Count: 1,
      m2Count: 1,
      m3Count: 1,
      createdAt: '2026-03-13T09:00:00',
    },
  ];
  let simulationBatchStatus = {
    simulationBatchId: 41,
    name: 'SIM_BATCH',
    scenarioName: 'SCN_STANDARD',
    aircraftCount: 3,
    m1Count: 1,
    m2Count: 1,
    m3Count: 1,
    diceStrategy: 'RANDOM',
    requestedRuns: 1,
    completedRuns: 1,
    failedRuns: 0,
    wonRuns: 1,
    lostRuns: 0,
    status: 'COMPLETED',
    currentGameName: null,
  };
  let scenarios = [
    {
      scenarioId: 1,
      name: 'SCN_STANDARD',
      version: 'V7',
      sourceType: 'SYSTEM',
      editable: false,
      deletable: false,
      published: true,
    },
    {
      scenarioId: 2,
      name: 'WINTER_OPS',
      version: 'V7',
      sourceType: 'USER',
      editable: true,
      deletable: true,
      published: false,
    },
  ];

  global.fetch = jest.fn((url, options = {}) => {
    if (String(url).includes('/reference/rules')) {
      return jsonResponse({
        initialSetup: { aircraftCount: 3 },
        missions: [
          { code: 'M1', name: 'Recon', flightHours: 4, fuelCost: 20, weaponCost: 0 },
          { code: 'M2', name: 'Strike', flightHours: 6, fuelCost: 30, weaponCost: 2 },
          { code: 'M3', name: 'Deep Strike', flightHours: 8, fuelCost: 40, weaponCost: 4 },
        ],
      });
    }

    if (String(url).endsWith('/scenarios') && (!options.method || options.method === 'GET')) {
      return jsonResponse(scenarios);
    }

    if (String(url).endsWith('/scenarios/1') && (!options.method || options.method === 'GET')) {
      return jsonResponse({
        scenarioId: 1,
        name: 'SCN_STANDARD',
        version: 'V7',
        description: 'Seeded standard scenario',
        sourceType: 'SYSTEM',
        editable: false,
        deletable: false,
        published: true,
        bases: [
          { code: 'A', name: 'Base A', baseTypeCode: 'MAIN', parkingCapacity: 4, maintenanceCapacity: 2 },
          { code: 'B', name: 'Base B', baseTypeCode: 'ROAD', parkingCapacity: 2, maintenanceCapacity: 1 },
          { code: 'C', name: 'Base C', baseTypeCode: 'FUEL', parkingCapacity: 2, maintenanceCapacity: 0, supplyRules: [] },
        ],
        aircraft: [
          { code: 'F1', aircraftTypeCode: 'JAS39', startBaseCode: 'A', fuelStart: 100, weaponsStart: 6, flightHoursStart: 20 },
          { code: 'F2', aircraftTypeCode: 'JAS39', startBaseCode: 'A', fuelStart: 100, weaponsStart: 6, flightHoursStart: 20 },
          { code: 'F3', aircraftTypeCode: 'JAS39', startBaseCode: 'B', fuelStart: 100, weaponsStart: 6, flightHoursStart: 20 },
        ],
        missions: [
          { code: 'M1', missionTypeCode: 'M1', missionTypeName: 'Recon', sortOrder: 1, defaultCount: 1, fuelCost: 20, weaponCost: 0, flightTimeCost: 4 },
          { code: 'M2', missionTypeCode: 'M2', missionTypeName: 'Strike', sortOrder: 2, defaultCount: 1, fuelCost: 30, weaponCost: 2, flightTimeCost: 6 },
          { code: 'M3', missionTypeCode: 'M3', missionTypeName: 'Deep Strike', sortOrder: 3, defaultCount: 1, fuelCost: 40, weaponCost: 4, flightTimeCost: 8 },
        ],
        diceRules: [
          { diceValue: 1, damageType: 'DESTROYED', repairRounds: 0 },
          { diceValue: 2, damageType: 'FULL_SERVICE_REQUIRED', repairRounds: 4 },
          { diceValue: 3, damageType: 'MAJOR_REPAIR', repairRounds: 3 },
        ],
      });
    }

    if (String(url).endsWith('/scenarios/2') && (!options.method || options.method === 'GET')) {
      return jsonResponse({
        scenarioId: 2,
        name: 'WINTER_OPS',
        version: 'V7',
        description: 'Custom scenario',
        sourceType: 'USER',
        editable: true,
        deletable: true,
        published: false,
        bases: [
          {
            code: 'A',
            name: 'Base A',
            baseTypeCode: 'MAIN',
            parkingCapacity: 6,
            maintenanceCapacity: 3,
            fuelStart: 150,
            fuelMax: 220,
            weaponsStart: 12,
            weaponsMax: 18,
            sparePartsStart: 5,
            sparePartsMax: 9,
            supplyRules: [
              { resource: 'FUEL', frequencyRounds: 2, deliveryAmount: 40 },
              { resource: 'SPARE_PARTS', frequencyRounds: 3, deliveryAmount: 2 },
            ],
          },
          {
            code: 'C',
            name: 'Base C',
            baseTypeCode: 'FUEL',
            parkingCapacity: 2,
            maintenanceCapacity: 0,
            fuelStart: 60,
            fuelMax: 120,
            weaponsStart: 0,
            weaponsMax: 0,
            sparePartsStart: 0,
            sparePartsMax: 0,
            supplyRules: [
              { resource: 'FUEL', frequencyRounds: 2, deliveryAmount: 35 },
              { resource: 'WEAPONS', frequencyRounds: 2, deliveryAmount: 0 },
              { resource: 'SPARE_PARTS', frequencyRounds: 3, deliveryAmount: 0 },
            ],
          },
        ],
        aircraft: [{ code: 'F1', aircraftTypeCode: 'JAS39', startBaseCode: 'A', fuelStart: 100, weaponsStart: 6, flightHoursStart: 20 }],
        missions: [{ code: 'M1', missionTypeCode: 'M1', missionTypeName: 'Recon', sortOrder: 1, defaultCount: 1, fuelCost: 20, weaponCost: 0, flightTimeCost: 4 }],
        diceRules: [
          { diceValue: 1, damageType: 'DESTROYED', repairRounds: 0 },
        ],
      });
    }

    if (String(url).endsWith('/scenarios/2') && options.method === 'PUT') {
      const payload = JSON.parse(options.body);
      return jsonResponse({
        scenarioId: 2,
        name: 'WINTER_OPS',
        version: 'V7',
        description: 'Custom scenario',
        sourceType: 'USER',
        editable: true,
        deletable: true,
        published: false,
        bases: payload.bases,
        aircraft: payload.aircraft,
        missions: payload.missions,
        diceRules: [
          { diceValue: 1, damageType: 'DESTROYED', repairRounds: 0 },
        ],
      });
    }

    if (String(url).endsWith('/scenarios/1/duplicate') && options.method === 'POST') {
      scenarios = [
        ...scenarios,
        {
          scenarioId: 3,
          name: 'SCN_STANDARD_COPY',
          version: 'V7',
          sourceType: 'USER',
          editable: true,
          deletable: true,
          published: false,
        },
      ];
      return jsonResponse({
        scenarioId: 3,
        name: 'SCN_STANDARD_COPY',
        version: 'V7',
        description: 'Seeded standard scenario',
        sourceType: 'USER',
        editable: true,
        deletable: true,
        published: false,
        bases: [],
        aircraft: [{ code: 'F1', aircraftTypeCode: 'JAS39', startBaseCode: 'A', fuelStart: 100, weaponsStart: 6, flightHoursStart: 20 }],
        missions: [{ code: 'M1', missionTypeCode: 'M1', missionTypeName: 'Recon', sortOrder: 1, defaultCount: 1, fuelCost: 20, weaponCost: 0, flightTimeCost: 4 }],
        diceRules: [],
      });
    }

    if (String(url).endsWith('/scenarios/2') && options.method === 'DELETE') {
      scenarios = scenarios.filter((scenario) => scenario.scenarioId !== 2);
      return jsonResponse({
        success: true,
        message: 'Scenario deleted',
      });
    }

    if (String(url).endsWith('/scenarios/3') && (!options.method || options.method === 'GET')) {
      return jsonResponse({
        scenarioId: 3,
        name: 'SCN_STANDARD_COPY',
        version: 'V7',
        description: 'Seeded standard scenario',
        sourceType: 'USER',
        editable: true,
        deletable: true,
        published: false,
        bases: [],
        aircraft: [{ code: 'F1', aircraftTypeCode: 'JAS39', startBaseCode: 'A', fuelStart: 100, weaponsStart: 6, flightHoursStart: 20 }],
        missions: [{ code: 'M1', missionTypeCode: 'M1', missionTypeName: 'Recon', sortOrder: 1, defaultCount: 1, fuelCost: 20, weaponCost: 0, flightTimeCost: 4 }],
        diceRules: [],
      });
    }

    if (String(url).endsWith('/scenarios/1/create-game') && options.method === 'POST') {
      const payload = JSON.parse(options.body);
      if (payload.gameName === 'TAKEN_NAME') {
        return Promise.resolve({
          ok: false,
          text: () => Promise.resolve(JSON.stringify({ message: 'The game name "TAKEN_NAME" is already in use. Choose a different name.' })),
        });
      }
      currentGameName = payload.gameName || 'GAME_001';
      return jsonResponse({
        gameId: 11,
        name: currentGameName,
        scenarioName: 'SCN_STANDARD',
        scenarioVersion: 'V7',
        status: 'ACTIVE',
        currentRound: 0,
        roundPhase: null,
        roundOpen: false,
        canStartRound: true,
        canCompleteRound: false,
      });
    }

    if (String(url).endsWith('/scenarios/2/create-game') && options.method === 'POST') {
      const payload = JSON.parse(options.body);
      currentGameName = payload.gameName || 'GAME_001';
      return jsonResponse({
        gameId: 11,
        name: currentGameName,
        scenarioName: 'WINTER_OPS',
        scenarioVersion: 'V7',
        status: 'ACTIVE',
        currentRound: 0,
        roundPhase: null,
        roundOpen: false,
        canStartRound: true,
        canCompleteRound: false,
      });
    }

    if (String(url).endsWith('/scenarios/3/create-game') && options.method === 'POST') {
      const payload = JSON.parse(options.body);
      currentGameName = payload.gameName || 'SCN_STANDARD_COPY_TEST';
      return jsonResponse({
        gameId: 31,
        name: currentGameName,
        scenarioName: 'SCN_STANDARD_COPY',
        scenarioVersion: 'V7',
        status: 'ACTIVE',
        currentRound: 0,
        roundPhase: null,
        roundOpen: false,
        canStartRound: true,
        canCompleteRound: false,
      });
    }

    if (String(url).endsWith('/simulations') && options.method === 'POST') {
      const payload = JSON.parse(options.body);
      simulationBatchStatus = {
        simulationBatchId: 41,
        name: payload.batchName,
        scenarioName: payload.scenarioName || 'SCN_STANDARD',
        aircraftCount: payload.aircraftCount || 3,
        m1Count: payload.missionTypeCounts?.M1 || 0,
        m2Count: payload.missionTypeCounts?.M2 || 0,
        m3Count: payload.missionTypeCounts?.M3 || 0,
        diceStrategy: payload.diceStrategy || 'RANDOM',
        requestedRuns: payload.runCount || 1,
        completedRuns: 0,
        failedRuns: 0,
        wonRuns: 0,
        lostRuns: 0,
        status: 'PENDING',
        currentGameName: null,
      };
      return jsonResponse(simulationBatchStatus);
    }

    if (String(url).endsWith('/simulations/41') && (!options.method || options.method === 'GET')) {
      simulationBatchStatus = {
        ...simulationBatchStatus,
        completedRuns: simulationBatchStatus.requestedRuns,
        failedRuns: 0,
        wonRuns: simulationBatchStatus.requestedRuns,
        lostRuns: 0,
        status: 'COMPLETED',
        currentGameName: null,
      };
      return jsonResponse(simulationBatchStatus);
    }

    if (String(url).includes('/analytics/games') && (!options.method || options.method === 'GET')) {
      const parsedUrl = new URL(String(url));
      const filtered = analyticsRows.filter((row) => {
        const scenarioName = parsedUrl.searchParams.get('scenarioName');
        const createdDate = parsedUrl.searchParams.get('createdDate');
        const aircraftCount = parsedUrl.searchParams.get('aircraftCount');
        const m1Count = parsedUrl.searchParams.get('m1Count');
        const m2Count = parsedUrl.searchParams.get('m2Count');
        const m3Count = parsedUrl.searchParams.get('m3Count');
        if (scenarioName && row.scenarioName !== scenarioName) {
          return false;
        }
        if (createdDate && !String(row.createdAt).startsWith(createdDate)) {
          return false;
        }
        if (aircraftCount && Number(row.aircraftCount) !== Number(aircraftCount)) {
          return false;
        }
        if (m1Count && Number(row.m1Count) !== Number(m1Count)) {
          return false;
        }
        if (m2Count && Number(row.m2Count) !== Number(m2Count)) {
          return false;
        }
        if (m3Count && Number(row.m3Count) !== Number(m3Count)) {
          return false;
        }
        return true;
      });
      return jsonResponse(filtered);
    }

    if (String(url).endsWith('/games/11') && (!options.method || options.method === 'GET')) {
      return jsonResponse({
        ...runtimeState({
          round: 0,
          roundPhase: null,
          roundOpen: false,
          canStartRound: true,
        }),
        game: {
          gameId: 11,
          name: currentGameName,
          scenarioName: 'SCN_STANDARD',
          scenarioVersion: 'V7',
          status: 'ACTIVE',
          currentRound: 0,
          roundPhase: null,
          roundOpen: false,
          canStartRound: true,
          canCompleteRound: false,
        },
      });
    }

    if (String(url).endsWith('/games/11/dice-rolls/auto') && options.method === 'POST') {
      return jsonResponse({
        gameState: runtimeState({
          round: 1,
          roundPhase: 'LANDING',
          roundOpen: true,
          canStartRound: false,
          aircraft: [
            { code: 'F1', status: 'AWAITING_LANDING', currentBase: null, fuel: 70, weapons: 4, remainingFlightHours: 12, damage: 'COMPONENT_DAMAGE', lastDiceValue: 4, allowedActions: ['LAND', 'SEND_TO_HOLDING'] },
          ],
        }),
        roundCompleted: false,
        autoAssignments: [],
        autoLandings: [],
        pendingDiceAircraft: [],
        nextAction: 'LAND_AIRCRAFT',
        messages: ['Dice roll recorded for F1'],
        gameFinished: false,
      });
    }

    if (String(url).endsWith('/games/31') && (!options.method || options.method === 'GET')) {
      return jsonResponse({
        ...runtimeState({
          round: 0,
          roundPhase: null,
          roundOpen: false,
          canStartRound: true,
        }),
        game: {
          gameId: 31,
          name: 'SCN_STANDARD_COPY_TEST',
          scenarioName: 'SCN_STANDARD_COPY',
          scenarioVersion: 'V7',
          status: 'ACTIVE',
          currentRound: 0,
          roundPhase: null,
          roundOpen: false,
          canStartRound: true,
          canCompleteRound: false,
        },
      });
    }

    if (String(url).endsWith('/games/11/rounds/plan') && options.method === 'POST') {
      return jsonResponse({
        gameState: runtimeState({
          round: 1,
          roundPhase: 'PLANNING',
          roundOpen: true,
          canStartRound: false,
          aircraft: [
            aircraft('F1', 'ON_MISSION', null, 'M3'),
            aircraft('F2', 'ON_MISSION', null, 'M1'),
            aircraft('F3', 'READY', 'A', null),
          ],
          missions: [
            mission('M1-1', 'M1', 'ASSIGNED'),
            mission('M2-1', 'M2', 'AVAILABLE'),
            mission('M3-1', 'M3', 'ASSIGNED'),
          ],
        }),
        nextAction: 'RESOLVE_MISSIONS',
        roundCompleted: false,
        gameFinished: false,
        pendingDiceAircraft: [],
        autoAssignments: ['F1 -> M3-1', 'F2 -> M1-1'],
        autoLandings: [],
        messages: ['Round started'],
      });
    }

    if (String(url).endsWith('/games/11/missions/resolve-auto') && options.method === 'POST') {
      return jsonResponse({
        gameState: runtimeState({
          round: 1,
          roundPhase: 'DICE_ROLL',
          roundOpen: true,
          canStartRound: false,
          aircraft: [
            aircraft('F1', 'AWAITING_DICE_ROLL', null, null),
            aircraft('F2', 'AWAITING_DICE_ROLL', null, null),
            aircraft('F3', 'READY', 'A', null),
          ],
          missions: [
            mission('M1-1', 'M1', 'COMPLETED'),
            mission('M2-1', 'M2', 'AVAILABLE'),
            mission('M3-1', 'M3', 'COMPLETED'),
          ],
        }),
        nextAction: 'ROLL_DICE',
        roundCompleted: false,
        gameFinished: false,
        pendingDiceAircraft: ['F1', 'F2'],
        autoAssignments: [],
        autoLandings: [],
        messages: ['Mission resolution completed'],
      });
    }

    if (String(url).endsWith('/games/11/abort') && options.method === 'POST') {
      return jsonResponse({
        success: true,
        message: 'Game aborted',
      });
    }

    return jsonResponse({});
  });
});

test('renders smart air base shell', async () => {
  render(<App />);
  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });
  expect(screen.getByText(/Smart Air Base/i)).toBeInTheDocument();
});

test('loads mission cards from rules endpoint', async () => {
  render(<App />);

  await waitFor(() => {
    expect(screen.getByText(/M1-1 - Recon/i)).toBeInTheDocument();
  });
});

test('shows automation wait settings in the configured order', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByLabelText('Automated'));

  const previewInput = screen.getByLabelText('Mission preview seconds');
  const diceInput = screen.getByLabelText('Seconds before dice roll');
  const nextRoundInput = screen.getByLabelText('Seconds before next round');

  expect(previewInput.compareDocumentPosition(diceInput) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  expect(diceInput.compareDocumentPosition(nextRoundInput) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
});

test('manual flow uses plan then resolve missions endpoints', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Create game'));
  fireEvent.click(screen.getByText('Use default name'));

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/games/11',
      expect.objectContaining({ headers: { 'Content-Type': 'application/json' } })
    );
  });

  fireEvent.click(screen.getByText('Next turn'));

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/games/11/rounds/plan',
      expect.objectContaining({ method: 'POST' })
    );
  });

  expect(screen.getByText('F1')).toBeInTheDocument();
  expect(screen.getByText('Mission: M3')).toBeInTheDocument();

  fireEvent.click(screen.getByText('Resolve missions'));

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/games/11/missions/resolve-auto',
      expect.objectContaining({ method: 'POST' })
    );
  });

  expect(screen.getByText('Awaiting dice roll')).toBeInTheDocument();
  expect(screen.getByText('Mission resolution completed. Next action: Roll dice.')).toBeInTheDocument();
});

test('selected scenario in editor is used when creating a game in play mode', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Scenario editor'));
  fireEvent.click(screen.getByText('WINTER_OPS'));
  fireEvent.click(screen.getByText('Play'));

  await waitFor(() => {
    expect(screen.getByDisplayValue('WINTER_OPS')).toBeInTheDocument();
  });

  fireEvent.click(screen.getByText('Create game'));
  fireEvent.click(screen.getByText('Use default name'));

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/scenarios/2/create-game',
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('"maxRounds":1000'),
      })
    );
  });

  const createGameCall = global.fetch.mock.calls.find(([url, options]) => url === 'http://localhost:8080/api/scenarios/2/create-game' && options?.method === 'POST');
  expect(createGameCall[1].body).toContain('"aircraftCount":1');
});

test('show scenario rules reflects the selected scenario data', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Scenario editor'));
  fireEvent.click(screen.getByText('WINTER_OPS'));
  fireEvent.click(screen.getByText('Play'));
  fireEvent.click(screen.getByText('Show scenario rules'));

  await waitFor(() => {
    expect(screen.getByText('WINTER_OPS')).toBeInTheDocument();
  });

  expect(screen.getByText('Custom scenario')).toBeInTheDocument();
  expect(screen.getByText(/Total base capacity in this scenario is 8 parking slots and 3 maintenance slots./i)).toBeInTheDocument();
  expect(screen.getByText(/Dice outcomes: 1 Destroyed./i)).toBeInTheDocument();
});

test('create game supports a custom game name', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Create game'));
  fireEvent.click(screen.getByText('Enter name'));
  fireEvent.change(screen.getByLabelText('Game name'), { target: { value: 'My named game' } });
  fireEvent.click(screen.getByText('Create named game'));

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/scenarios/1/create-game',
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('"gameName":"My named game"'),
      })
    );
  });

  await waitFor(() => {
    expect(screen.getByText('Game created My named game (11).')).toBeInTheDocument();
  });
  expect(screen.getByText('My named game created with Game ID 11.')).toBeInTheDocument();
  expect(screen.getByLabelText('Current game name')).toHaveValue('My named game');
  expect(screen.getByText(/Time: .* \| Round: 0/i)).toBeInTheDocument();
  expect(screen.getByText(/Round: 0/)).toBeInTheDocument();
});

test('custom game name cannot be empty when chosen', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Create game'));
  fireEvent.click(screen.getByText('Enter name'));

  expect(screen.getByText(/Game name is required when you choose a custom name./i)).toBeInTheDocument();
  expect(screen.getByText('Create named game')).toBeDisabled();
});

test('create game is disabled during an ongoing game and abort game is only enabled then', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  expect(screen.getByText('Create game')).not.toBeDisabled();
  expect(screen.getAllByText('Abort game')[0]).toBeDisabled();

  fireEvent.click(screen.getByText('Create game'));
  fireEvent.click(screen.getByText('Use default name'));

  await waitFor(() => {
    expect(screen.getByText('Game created GAME_001 (11).')).toBeInTheDocument();
  });

  expect(screen.getByText('Create game')).toBeDisabled();
  expect(screen.getAllByText('Abort game')[0]).not.toBeDisabled();
});

test('scenario editor tab is disabled while an active game is running', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  const scenarioEditorTab = screen.getByRole('tab', { name: 'Scenario editor' });
  const simulatorTab = screen.getByRole('tab', { name: 'Simulator' });
  expect(scenarioEditorTab).not.toBeDisabled();
  expect(simulatorTab).not.toBeDisabled();

  fireEvent.click(screen.getByText('Create game'));
  fireEvent.click(screen.getByText('Use default name'));

  await waitFor(() => {
    expect(screen.getByText('Game created GAME_001 (11).')).toBeInTheDocument();
  });

  expect(scenarioEditorTab).toBeDisabled();
  expect(simulatorTab).toBeDisabled();
  expect(screen.queryByText('Scenario library')).not.toBeInTheDocument();
});

test('simulator locks play and scenario editor while a batch is running', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  const originalFetch = global.fetch;
  let resolveStatus;
  const delayedStatus = new Promise((resolve) => {
    resolveStatus = resolve;
  });
  global.fetch = jest.fn((url, options = {}) => {
    if (
      String(url) === 'http://localhost:8080/api/simulations/41'
      && (!options?.method || options?.method === 'GET')
    ) {
      return delayedStatus;
    }
    return originalFetch(url, options);
  });

  fireEvent.click(screen.getByText('Simulator'));
  const simulatorForm = screen.getByText('Start simulation').closest('form');
  expect(simulatorForm).toBeTruthy();
  const simulatorInputs = simulatorForm.querySelectorAll('input');
  fireEvent.change(simulatorInputs[0], { target: { value: 'batch' } });
  fireEvent.change(simulatorInputs[1], { target: { value: '1' } });
  fireEvent.click(screen.getByText('Start simulation'));

  await waitFor(() => {
    expect(screen.getByRole('tab', { name: 'Play' })).toBeDisabled();
    expect(screen.getByRole('tab', { name: 'Scenario editor' })).toBeDisabled();
  });

  resolveStatus(jsonResponse({
    simulationBatchId: 41,
    name: 'BATCH',
    scenarioName: 'SCN_STANDARD',
    aircraftCount: 3,
    m1Count: 1,
    m2Count: 1,
    m3Count: 1,
    diceStrategy: 'RANDOM',
    maxRounds: 1000,
    requestedRuns: 1,
    completedRuns: 1,
    failedRuns: 0,
    wonRuns: 1,
    lostRuns: 0,
    status: 'COMPLETED',
    currentGameName: null,
  }));

  await waitFor(() => {
    expect(screen.getByText('Simulation batch completed. 1/1 games saved.')).toBeInTheDocument();
  });

  const resultsPanel = screen.getByText('Simulation results').closest('section');
  expect(resultsPanel).toBeTruthy();
  const results = within(resultsPanel);
  expect(results.getByText('Scenario')).toBeInTheDocument();
  expect(results.getByText('SCN_STANDARD')).toBeInTheDocument();
  expect(results.getByText('Won')).toBeInTheDocument();
  expect(results.getByText('Lost')).toBeInTheDocument();
  expect(results.getByText('Failed')).toBeInTheDocument();

  const simulatorCreateCall = global.fetch.mock.calls.find(([url, options]) =>
    String(url) === 'http://localhost:8080/api/simulations'
      && options?.method === 'POST'
      && String(options?.body || '').includes('"batchName":"BATCH"')
  );
  expect(simulatorCreateCall).toBeTruthy();
  expect(String(simulatorCreateCall[1]?.body || '')).toContain('"maxRounds":1000');
  expect(screen.getByRole('tab', { name: 'Play' })).not.toBeDisabled();
  expect(screen.getByRole('tab', { name: 'Scenario editor' })).not.toBeDisabled();
});

test('dashboard shows latest analytics rows first and filters by scenario', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByRole('tab', { name: 'Dashboard' }));

  await waitFor(() => {
    expect(screen.getByText('GAME_002')).toBeInTheDocument();
    expect(screen.getByText('GAME_001')).toBeInTheDocument();
  });

  expect(screen.getByText('Page 1 of 1 · 2 total rows')).toBeInTheDocument();

  const bodyRows = document.querySelectorAll('.dashboard-table tbody tr');
  expect(bodyRows[0].textContent).toContain('GAME_002');

  fireEvent.change(screen.getByLabelText('Scenario'), { target: { value: 'SCN_STANDARD' } });

  await waitFor(() => {
    expect(screen.getByText('GAME_001')).toBeInTheDocument();
    expect(screen.queryByText('GAME_002')).not.toBeInTheDocument();
  });
});

test('dashboard can export filtered analytics rows as csv', async () => {
  const originalShowSaveFilePicker = window.showSaveFilePicker;
  const write = jest.fn().mockResolvedValue(undefined);
  const close = jest.fn().mockResolvedValue(undefined);
  const createWritable = jest.fn().mockResolvedValue({ write, close });
  window.showSaveFilePicker = jest.fn().mockResolvedValue({ createWritable });

  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByRole('tab', { name: 'Dashboard' }));

  await waitFor(() => {
    expect(screen.getByText('GAME_002')).toBeInTheDocument();
  });

  fireEvent.change(screen.getByLabelText('CSV file name'), { target: { value: 'analytics_export' } });
  fireEvent.click(screen.getByText('Export CSV'));

  await waitFor(() => {
    expect(window.showSaveFilePicker).toHaveBeenCalledWith(expect.objectContaining({
      suggestedName: 'analytics_export.csv',
    }));
    expect(createWritable).toHaveBeenCalled();
    expect(write).toHaveBeenCalled();
    expect(close).toHaveBeenCalled();
    expect(screen.getByText('Exported 2 rows to analytics_export.csv.')).toBeInTheDocument();
  });

  window.showSaveFilePicker = originalShowSaveFilePicker;
});

test('custom game name must be unique', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Create game'));
  fireEvent.click(screen.getByText('Enter name'));
  fireEvent.change(screen.getByLabelText('Game name'), { target: { value: 'TAKEN_NAME' } });
  fireEvent.click(screen.getByText('Create named game'));

  await waitFor(() => {
    expect(screen.getByText('The game name "TAKEN_NAME" is already in use. Choose a different name.')).toBeInTheDocument();
  });

  expect(screen.queryByText(/Game created TAKEN_NAME/i)).not.toBeInTheDocument();
});

test('raw tool wrapper errors are shown as readable messages', async () => {
  global.fetch.mockImplementationOnce(() => jsonResponse({
    initialSetup: { aircraftCount: 3 },
    missions: [
      { code: 'M1', name: 'Recon', flightHours: 4, fuelCost: 20, weaponCost: 0 },
      { code: 'M2', name: 'Strike', flightHours: 6, fuelCost: 30, weaponCost: 2 },
      { code: 'M3', name: 'Deep Strike', flightHours: 8, fuelCost: 40, weaponCost: 4 },
    ],
  }));

  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  global.fetch.mockImplementation((url, options = {}) => {
    if (String(url).includes('/reference/rules')) {
      return jsonResponse({
        initialSetup: { aircraftCount: 3 },
        missions: [
          { code: 'M1', name: 'Recon', flightHours: 4, fuelCost: 20, weaponCost: 0 },
          { code: 'M2', name: 'Strike', flightHours: 6, fuelCost: 30, weaponCost: 2 },
          { code: 'M3', name: 'Deep Strike', flightHours: 8, fuelCost: 40, weaponCost: 4 },
        ],
      });
    }
    if (String(url).endsWith('/scenarios') && (!options.method || options.method === 'GET')) {
      return jsonResponse([
        {
          scenarioId: 1,
          name: 'SCN_STANDARD',
          version: 'V7',
          sourceType: 'SYSTEM',
          editable: false,
          deletable: false,
          published: true,
        },
      ]);
    }
    if (String(url).endsWith('/scenarios/1') && (!options.method || options.method === 'GET')) {
      return jsonResponse({
        scenarioId: 1,
        name: 'SCN_STANDARD',
        version: 'V7',
        description: 'Seeded standard scenario',
        sourceType: 'SYSTEM',
        editable: false,
        deletable: false,
        published: true,
        bases: [],
        aircraft: [],
        missions: [],
        diceRules: [],
      });
    }
    if (String(url).endsWith('/scenarios/1/create-game') && options.method === 'POST') {
      return Promise.resolve({
        ok: false,
        text: () => Promise.resolve(JSON.stringify({
          message: 'Error calling tool: [TextContent[annotations=null, text=The game name "RAW_DUPLICATE" is already in use. Choose a different name., meta=null]]',
        })),
      });
    }
    return jsonResponse({});
  });

  fireEvent.click(screen.getByText('Create game'));
  fireEvent.click(screen.getByText('Enter name'));
  fireEvent.change(screen.getByLabelText('Game name'), { target: { value: 'RAW_DUPLICATE' } });
  fireEvent.click(screen.getByText('Create named game'));

  await waitFor(() => {
    expect(screen.getByText('The game name "RAW_DUPLICATE" is already in use. Choose a different name.')).toBeInTheDocument();
  });
  expect(screen.queryByText(/Error calling tool:/i)).not.toBeInTheDocument();
});

test('starting a new create flow from startup clears prior status and event history', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Create game'));
  fireEvent.click(screen.getByText('Enter name'));
  fireEvent.change(screen.getByLabelText('Game name'), { target: { value: 'My named game' } });
  fireEvent.click(screen.getByText('Create named game'));

  await waitFor(() => {
    expect(screen.getByText('Game created My named game (11).')).toBeInTheDocument();
  });

  fireEvent.click(screen.getAllByText('Abort game')[0]);

  await waitFor(() => {
    expect(screen.getByText('Create a game to begin.')).toBeInTheDocument();
  });

  fireEvent.click(screen.getByText('Create game'));

  expect(screen.queryByText('Game created My named game (11).')).not.toBeInTheDocument();
  expect(screen.getByText('Create a game to begin.')).toBeInTheDocument();
  expect(screen.getByText('Use default name')).toBeInTheDocument();
});

test('play mode previews base layout from the selected scenario', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Scenario editor'));
  fireEvent.click(screen.getByText('WINTER_OPS'));
  fireEvent.click(screen.getByText('Play'));

  await waitFor(() => {
    expect(screen.getByText('Parking slots free 6 / 6')).toBeInTheDocument();
  });

  expect(screen.getByText('Repair slots free 3 / 3')).toBeInTheDocument();
  expect(screen.getByText('Max fuel 220 | Max weapons 18 | Max spare parts 9')).toBeInTheDocument();
});

test('abort game returns the app to its startup state and invalidates the current game in the client', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Create game'));
  fireEvent.click(screen.getByText('Use default name'));

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/games/11',
      expect.objectContaining({ headers: { 'Content-Type': 'application/json' } })
    );
  });

  fireEvent.change(screen.getByLabelText('Game ID'), { target: { value: '11' } });
  fireEvent.click(screen.getAllByText('Abort game')[0]);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/games/11/abort',
      expect.objectContaining({ method: 'POST' })
    );
  });

  await waitFor(() => {
    expect(screen.getByDisplayValue('')).toBeInTheDocument();
  });

  expect(screen.getByText('Create a game to begin.')).toBeInTheDocument();
  expect(screen.getByText('Game status')).toBeInTheDocument();
  expect(screen.getByText('Not started')).toBeInTheDocument();
  expect(screen.getByLabelText('Current game name')).toHaveValue('No active game');
  expect(screen.getByText('No aircraft are currently waiting for a dice roll.')).toBeInTheDocument();
});

test('scenario browser can duplicate a scenario', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/scenarios',
      expect.objectContaining({ headers: { 'Content-Type': 'application/json' } })
    );
  });

  fireEvent.click(screen.getByText('Scenario editor'));

  await waitFor(() => {
    expect(screen.getByText('Scenario library')).toBeInTheDocument();
  });

  expect(screen.getAllByText('SCN_STANDARD')[0]).toBeInTheDocument();
  expect(screen.getByText('Seeded standard scenario')).toBeInTheDocument();

  fireEvent.click(screen.getByText('Duplicate'));

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/scenarios/1/duplicate',
      expect.objectContaining({ method: 'POST' })
    );
  });

  await waitFor(() => {
    expect(screen.getByDisplayValue('SCN_STANDARD_COPY')).toBeInTheDocument();
  });
});

test('standard scenario fields are read-only in scenario editor', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Scenario editor'));

  await waitFor(() => {
    expect(screen.getByDisplayValue('Seeded standard scenario')).toBeDisabled();
  });

  expect(screen.getByDisplayValue('SCN_STANDARD_COPY')).toBeEnabled();
});

test('duplicate scenario name is normalized to uppercase letters digits and underscores', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Scenario editor'));

  const duplicateInput = await screen.findByLabelText('Duplicate as');
  fireEvent.change(duplicateInput, { target: { value: 'winter ops-2!' } });

  expect(screen.getByDisplayValue('WINTEROPS2')).toBeInTheDocument();
});

test('duplicate scenario name cannot be empty', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Scenario editor'));

  const duplicateInput = await screen.findByLabelText('Duplicate as');
  fireEvent.change(duplicateInput, { target: { value: '' } });

  expect(screen.getByText(/Name is required and must contain at least 1 uppercase letter, digit, or underscore./i)).toBeInTheDocument();
  expect(screen.getByText('Duplicate')).toBeDisabled();
});

test('base c does not expose repair slots in scenario editor', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Scenario editor'));

  await waitFor(() => {
    expect(screen.getByText('Base settings')).toBeInTheDocument();
  });

  expect(screen.getAllByDisplayValue('0').length).toBeGreaterThan(0);
});

test('base c only allows fuel values to be changed in scenario editor', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Scenario editor'));
  fireEvent.click(screen.getByText('WINTER_OPS'));

  await waitFor(() => {
    expect(screen.getByText('Base settings')).toBeInTheDocument();
  });

  expect(screen.getByText(/Weapons and spare-parts stocks are fixed at 0 for Base C because this base type is refuel-only./i)).toBeInTheDocument();
  expect(screen.getByText(/Weapons and spare-parts deliveries do not apply to Base C in any scenario. This base remains fuel-only./i)).toBeInTheDocument();

  const baseCFuelStart = screen.getByDisplayValue('60');
  const baseCFuelMax = screen.getByDisplayValue('120');
  expect(baseCFuelStart).not.toBeDisabled();
  expect(baseCFuelMax).not.toBeDisabled();

  expect(screen.getAllByDisplayValue('0').length).toBeGreaterThan(4);
});

test('scenario overview shows base resource settings', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Scenario editor'));

  await waitFor(() => {
    expect(screen.getByText('Base resources')).toBeInTheDocument();
  });

  expect(screen.getByText(/Values are shown as start\/max per base./i)).toBeInTheDocument();
});

test('scenario browser can delete a custom scenario', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Scenario editor'));
  fireEvent.click(screen.getByText('WINTER_OPS'));

  await waitFor(() => {
    expect(screen.getByText('Custom scenario')).toBeInTheDocument();
  });

  fireEvent.click(screen.getByText('Delete scenario'));

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/scenarios/2',
      expect.objectContaining({ method: 'DELETE' })
    );
  });

  await waitFor(() => {
    expect(screen.queryByText('WINTER_OPS')).not.toBeInTheDocument();
  });

  expect(screen.getByText('SCN_STANDARD')).toBeInTheDocument();
  expect(screen.queryByText('Delete scenario')).not.toBeInTheDocument();
});

test('scenario editor can save changes for a custom scenario', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Scenario editor'));
  fireEvent.click(screen.getByText('WINTER_OPS'));

  await waitFor(() => {
    expect(screen.getByText('Save scenario')).toBeInTheDocument();
  });

  fireEvent.change(screen.getByDisplayValue('100'), { target: { value: '88' } });
  fireEvent.change(screen.getByDisplayValue('150'), { target: { value: '160' } });
  fireEvent.change(screen.getByDisplayValue('220'), { target: { value: '240' } });
  fireEvent.change(screen.getByDisplayValue('40'), { target: { value: '55' } });
  fireEvent.change(screen.getAllByDisplayValue('20')[1], { target: { value: '24' } });
  fireEvent.change(screen.getByDisplayValue('4'), { target: { value: '6' } });

  fireEvent.click(screen.getByText('Save scenario'));

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/scenarios/2',
      expect.objectContaining({
        method: 'PUT',
        body: expect.stringContaining('"fuelStart":88'),
      })
    );
  });

  expect(global.fetch).toHaveBeenCalledWith(
    'http://localhost:8080/api/scenarios/2',
    expect.objectContaining({
      method: 'PUT',
      body: expect.stringContaining('"fuelMax":240'),
    })
  );
  expect(global.fetch).toHaveBeenCalledWith(
    'http://localhost:8080/api/scenarios/2',
    expect.objectContaining({
      method: 'PUT',
      body: expect.stringContaining('"deliveryAmount":55'),
    })
  );
  expect(global.fetch).toHaveBeenCalledWith(
    'http://localhost:8080/api/scenarios/2',
    expect.objectContaining({
      method: 'PUT',
      body: expect.stringContaining('"fuelCost":24'),
    })
  );

  await waitFor(() => {
    expect(screen.getByText('Scenario saved.')).toBeInTheDocument();
  });
});

test('scenario editor can change initial aircraft count within total parking capacity', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Scenario editor'));
  fireEvent.click(screen.getByText('WINTER_OPS'));

  await waitFor(() => {
    expect(screen.getByText('Aircraft settings')).toBeInTheDocument();
  });

  fireEvent.change(screen.getByDisplayValue('1'), { target: { value: '3' } });
  fireEvent.click(screen.getByText('Save scenario'));

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/scenarios/2',
      expect.objectContaining({
        method: 'PUT',
        body: expect.stringContaining('"code":"F3"'),
      })
    );
  });
});

test('scenario editor keeps initial aircraft count at minimum 1', async () => {
  render(<App />);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });

  fireEvent.click(screen.getByText('Scenario editor'));
  fireEvent.click(screen.getByText('WINTER_OPS'));

  await waitFor(() => {
    expect(screen.getByText('Aircraft settings')).toBeInTheDocument();
  });

  const countInput = screen.getByDisplayValue('1');
  fireEvent.change(countInput, { target: { value: '0' } });

  expect(screen.getByDisplayValue('1')).toBeInTheDocument();
});

test('frontend dice selection helpers map manual and automated choices to analytics modes', () => {
  expect(manualDiceSelectionMode(false)).toBe('MANUAL_DIRECT_SELECTION');
  expect(manualDiceSelectionMode(true)).toBe('MANUAL_RANDOM_SELECTION');
  expect(automatedDiceSelectionMode('RANDOM')).toBe('AUTO_RANDOM');
  expect(automatedDiceSelectionMode('MIN_DAMAGE')).toBe('AUTO_MIN_DAMAGE');
  expect(automatedDiceSelectionMode('MAX_DAMAGE')).toBe('AUTO_MAX_DAMAGE');
});

function jsonResponse(data) {
  return Promise.resolve({
    ok: true,
    text: () => Promise.resolve(JSON.stringify(data)),
  });
}

function runtimeState({ round, roundPhase, roundOpen, canStartRound, aircraft = defaultAircraft(), missions = defaultMissions() }) {
  return {
    game: {
      gameId: 11,
      name: 'test',
      scenarioName: 'SCN_STANDARD',
      scenarioVersion: 'V7',
      status: 'ACTIVE',
      currentRound: round,
      roundPhase,
      roundOpen,
      canStartRound,
      canCompleteRound: false,
    },
    bases: [
      { code: 'A', name: 'Base A', fuelStock: 300, weaponsStock: 20, sparePartsStock: 10, parkingCapacity: 4, occupiedParkingSlots: 0, maintenanceCapacity: 2, occupiedMaintSlots: 0 },
      { code: 'B', name: 'Base B', fuelStock: 200, weaponsStock: 10, sparePartsStock: 4, parkingCapacity: 2, occupiedParkingSlots: 0, maintenanceCapacity: 1, occupiedMaintSlots: 0 },
      { code: 'C', name: 'Base C', fuelStock: 150, weaponsStock: 0, sparePartsStock: 0, parkingCapacity: 2, occupiedParkingSlots: 0, maintenanceCapacity: 0, occupiedMaintSlots: 0 },
    ],
    aircraft,
    missions,
    eventCount: 0,
    resourceTransactionCount: 0,
  };
}

function aircraft(code, status, currentBase, assignedMission) {
  return {
    code,
    status,
    currentBase,
    fuel: 100,
    weapons: 6,
    remainingFlightHours: 20,
    damage: 'NONE',
    repairRoundsRemaining: 0,
    inHolding: false,
    assignedMission,
    lastDiceValue: null,
    allowedActions: [],
  };
}

function mission(code, missionType, status) {
  return {
    code,
    missionType,
    status,
    sortOrder: 1,
    assignmentBlocker: null,
  };
}

function defaultAircraft() {
  return [
    aircraft('F1', 'READY', 'A', null),
    aircraft('F2', 'READY', 'A', null),
    aircraft('F3', 'READY', 'A', null),
  ];
}

function defaultMissions() {
  return [
    mission('M1-1', 'M1', 'AVAILABLE'),
    mission('M2-1', 'M2', 'AVAILABLE'),
    mission('M3-1', 'M3', 'AVAILABLE'),
  ];
}
