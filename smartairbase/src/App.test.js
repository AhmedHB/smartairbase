import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import App from './App';

beforeEach(() => {
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

    if (String(url).endsWith('/games') && options.method === 'POST') {
      return jsonResponse({
        gameId: 11,
        name: 'test',
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

    if (String(url).endsWith('/games/11') && (!options.method || options.method === 'GET')) {
      return jsonResponse(runtimeState({
        round: 0,
        roundPhase: null,
        roundOpen: false,
        canStartRound: true,
      }));
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
