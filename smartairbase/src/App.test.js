import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import App from './App';

beforeEach(() => {
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
          { code: 'A', name: 'Base A', baseTypeCode: 'MAIN', parkingCapacity: 6, maintenanceCapacity: 3, fuelStart: 150, fuelMax: 220, weaponsStart: 12, weaponsMax: 18, sparePartsStart: 5, sparePartsMax: 9 },
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

    if (String(url).endsWith('/scenarios/3/create-game') && options.method === 'POST') {
      return jsonResponse({
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
      });
    }

    if (String(url).endsWith('/games') && options.method === 'POST') {
      const payload = JSON.parse(options.body);
      return jsonResponse({
        gameId: 11,
        name: payload.gameName || 'GAME_001',
        scenarioName: payload.scenarioName || 'SCN_STANDARD',
        scenarioVersion: 'V7',
        status: 'ACTIVE',
        currentRound: 0,
        roundPhase: null,
        roundOpen: false,
        canStartRound: true,
        canCompleteRound: false,
      });
    }

    if (String(url).endsWith('/games/11') && (!options.method || options.method === 'GET')) {
      return jsonResponse(runtimeState({
        round: 0,
        roundPhase: null,
        roundOpen: false,
        canStartRound: true,
      }));
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
      'http://localhost:8080/api/games',
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('"scenarioName":"WINTER_OPS"'),
      })
    );
  });
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
  expect(screen.getByText(/Total base capacity in this scenario is 6 parking slots and 3 maintenance slots./i)).toBeInTheDocument();
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
      'http://localhost:8080/api/games',
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
      body: expect.stringContaining('"fuelCost":24'),
    })
  );

  await waitFor(() => {
    expect(screen.getByText('Scenario saved.')).toBeInTheDocument();
  });
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
