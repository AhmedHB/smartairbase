import './App.css';
import { useEffect, useMemo, useRef, useState } from 'react';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';

const INITIAL_CREATE_FORM = {
  scenarioName: 'SCN_STANDARD',
  gameName: '',
  maxRounds: 1000,
  aircraftCount: 3,
  missionTypeCounts: {
    M1: 1,
    M2: 1,
    M3: 1,
  },
};

const INITIAL_DICE_AUTOMATION = {
  mode: 'MANUAL',
  strategy: 'RANDOM',
  diceDelaySeconds: 5,
  nextRoundDelaySeconds: 5,
  missionPreviewSeconds: 5,
};

const INITIAL_SIMULATION_FORM = {
  batchName: 'SIM_BATCH',
  runCount: 10,
  maxRounds: 1000,
  scenarioName: 'SCN_STANDARD',
  aircraftCount: 3,
  missionTypeCounts: {
    M1: 1,
    M2: 1,
    M3: 1,
  },
  diceStrategy: 'RANDOM',
};

const INITIAL_DASHBOARD_FILTERS = {
  scenarioName: '',
  createdDate: '',
  aircraftCount: '',
  m1Count: '',
  m2Count: '',
  m3Count: '',
};
const DASHBOARD_PAGE_SIZE = 20;
const INITIAL_DASHBOARD_EXPORT_FILE_NAME = 'dashboard_export';

const BASE_TYPE_LABELS = {
  A: 'Main Airbase',
  B: 'Forward Base',
  C: 'Fuel Outpost',
};
const GRIPEN_IMAGE_URL = `${process.env.PUBLIC_URL || ''}/gripen.png`;
const TOOL_WRAPPER_MESSAGE_PATTERN = /text=([^,\]]+)/;
const SIMULATION_POLL_INTERVAL_MS = 500;

function isFuelOutpostScenarioBase(base) {
  const normalizedBaseCode = String(base?.code || '').trim().toUpperCase();
  const normalizedBaseTypeCode = String(base?.baseTypeCode || '').trim().toUpperCase();
  return normalizedBaseCode === 'C'
    || normalizedBaseCode === 'BASE_C'
    || normalizedBaseTypeCode === 'FUEL'
    || normalizedBaseTypeCode === 'C';
}

function sleep(milliseconds) {
  return new Promise((resolve) => {
    window.setTimeout(resolve, milliseconds);
  });
}

export function automatedDiceSelectionMode(strategy) {
  if (strategy === 'MIN_DAMAGE') {
    return 'AUTO_MIN_DAMAGE';
  }
  if (strategy === 'MAX_DAMAGE') {
    return 'AUTO_MAX_DAMAGE';
  }
  return 'AUTO_RANDOM';
}

export function manualDiceSelectionMode(useRandomDice) {
  return useRandomDice ? 'MANUAL_RANDOM_SELECTION' : 'MANUAL_DIRECT_SELECTION';
}

function App() {
  const [currentView, setCurrentView] = useState('PLAY');
  const [createForm, setCreateForm] = useState(INITIAL_CREATE_FORM);
  const [simulationForm, setSimulationForm] = useState(INITIAL_SIMULATION_FORM);
  const [dashboardFilters, setDashboardFilters] = useState(INITIAL_DASHBOARD_FILTERS);
  const [dashboardAllRows, setDashboardAllRows] = useState([]);
  const [diceAutomation, setDiceAutomation] = useState(INITIAL_DICE_AUTOMATION);
  const [gameId, setGameId] = useState('');
  const [rules, setRules] = useState(null);
  const [scenarios, setScenarios] = useState([]);
  const [selectedScenarioId, setSelectedScenarioId] = useState('');
  const [selectedScenario, setSelectedScenario] = useState(null);
  const [scenarioBusy, setScenarioBusy] = useState(false);
  const [duplicateScenarioName, setDuplicateScenarioName] = useState('');
  const [showScenarioRules, setShowScenarioRules] = useState(false);
  const [showColorLegend, setShowColorLegend] = useState(false);
  const [previousGameState, setPreviousGameState] = useState(null);
  const [gameState, setGameState] = useState(null);
  const [lastAutoResponse, setLastAutoResponse] = useState(null);
  const [selectedAircraft, setSelectedAircraft] = useState('');
  const [diceValue, setDiceValue] = useState(1);
  const [useRandomDice, setUseRandomDice] = useState(true);
  const [eventLog, setEventLog] = useState([]);
  const [analysisFeed, setAnalysisFeed] = useState([]);
  const [analysisPending, setAnalysisPending] = useState(false);
  const [postGameSummary, setPostGameSummary] = useState(null);
  const [status, setStatus] = useState({ kind: 'idle', message: 'Create a game to begin.' });
  const [simulationStatus, setSimulationStatus] = useState({
    running: false,
    completedRuns: 0,
    failedRuns: 0,
    wonRuns: 0,
    lostRuns: 0,
    requestedRuns: 0,
    currentGameName: '',
    batchName: '',
    scenarioName: '',
    aircraftCount: 0,
    missionTypeCounts: { M1: 0, M2: 0, M3: 0 },
    diceStrategy: 'RANDOM',
    maxRounds: 1000,
    message: 'Configure a simulation batch to run saved games without visual playback.',
  });
  const [simulationElapsedSeconds, setSimulationElapsedSeconds] = useState(0);
  const [dashboardRows, setDashboardRows] = useState([]);
  const [dashboardLoading, setDashboardLoading] = useState(false);
  const [dashboardPage, setDashboardPage] = useState(1);
  const [dashboardExportFileName, setDashboardExportFileName] = useState(INITIAL_DASHBOARD_EXPORT_FILE_NAME);
  const [dashboardExportStatus, setDashboardExportStatus] = useState({ kind: 'idle', message: '' });
  const hasValidDuplicateScenarioName = duplicateScenarioName.trim().length > 0;
  const [showCreateGamePrompt, setShowCreateGamePrompt] = useState(false);
  const [useCustomGameName, setUseCustomGameName] = useState(false);
  const simulationStartedAtRef = useRef(null);
  const automationInFlightRef = useRef(false);
  const nextRoundInFlightRef = useRef(false);
  const gameStateRef = useRef(null);
  const activeRequestControllersRef = useRef(new Set());
  const [nextRoundCountdown, setNextRoundCountdown] = useState(null);
  const missionPreviewTimerRef = useRef(null);
  const autoDiceTimerRef = useRef(null);
  const lastAnalysisRequestKeyRef = useRef(null);
  const lastFinalAnalysisRequestKeyRef = useRef(null);
  const analysisFeedListRef = useRef(null);
  const [missionPreviewActive, setMissionPreviewActive] = useState(false);
  const [manualMissionPreviewAssignments, setManualMissionPreviewAssignments] = useState({});

  const pendingDiceAircraft = useMemo(() => {
    if (!gameState?.aircraft) {
      return [];
    }
    return gameState.aircraft.filter((aircraft) => aircraft.status === 'AWAITING_DICE_ROLL');
  }, [gameState]);

  const holdingAircraft = useMemo(() => {
    if (!gameState?.aircraft) {
      return [];
    }
    return gameState.aircraft.filter((aircraft) => aircraft.status === 'HOLDING');
  }, [gameState]);

  const destroyedAircraft = useMemo(() => {
    if (!gameState?.aircraft) {
      return [];
    }
    return gameState.aircraft.filter((aircraft) => aircraft.status === 'CRASHED' || aircraft.status === 'DESTROYED');
  }, [gameState]);

  const automationEnabled = diceAutomation.mode === 'AUTOMATED';

  const onMissionAircraft = useMemo(() => {
    const actualOnMission = (gameState?.aircraft || []).filter((aircraft) => aircraft.status === 'ON_MISSION');
    if (!automationEnabled && pendingDiceAircraft.length) {
      return actualOnMission;
    }
    const pendingByCode = Object.fromEntries(pendingDiceAircraft.map((aircraft) => [aircraft.code, aircraft]));
    const syntheticPreview = Object.entries(manualMissionPreviewAssignments)
      .map(([aircraftCode, missionCode]) => {
        const pendingAircraft = pendingByCode[aircraftCode];
        if (!pendingAircraft) {
          return null;
        }
        return {
          ...pendingAircraft,
          status: 'ON_MISSION',
          assignedMission: missionCode,
        };
      })
      .filter(Boolean);

    return [...actualOnMission, ...syntheticPreview];
  }, [automationEnabled, gameState, manualMissionPreviewAssignments, pendingDiceAircraft]);

  const nextStep = useMemo(() => {
    if (!isValidGameId(gameId) || !gameState) {
      return 'CREATE_GAME';
    }
    if (missionPreviewActive && automationEnabled) {
      return 'NONE';
    }
    if (gameState?.game?.status && gameState.game.status !== 'ACTIVE') {
      return 'NONE';
    }
    // Manual mode deliberately stops after planning so the user can inspect "On mission"
    // before moving the round into dice resolution.
    if (!automationEnabled && gameState?.game?.roundOpen && gameState?.game?.roundPhase === 'PLANNING') {
      return 'RESOLVE_MISSIONS';
    }
    if (pendingDiceAircraft.length) {
      return 'ROLL_DICE';
    }
    if (gameState?.game?.canStartRound || lastAutoResponse?.nextAction === 'START_NEXT_ROUND') {
      return 'NEXT_TURN';
    }
    return 'NONE';
  }, [automationEnabled, gameId, gameState, lastAutoResponse, missionPreviewActive, pendingDiceAircraft]);

  const canStartNextTurn = nextStep === 'NEXT_TURN';
  const canResolveMissions = nextStep === 'RESOLVE_MISSIONS';
  const canRollDice = nextStep === 'ROLL_DICE';
  // The control panel allows exactly one active game at a time. Create is locked
  // while a live game is running, and abort only becomes available for that state.
  const hasOngoingGame = isValidGameId(gameId) && gameState?.game?.status === 'ACTIVE';
  const workspaceLocked = hasOngoingGame || simulationStatus.running;
  const selectedScenarioRules = useMemo(
    () => scenarioRulesFor(selectedScenario, createForm.scenarioName),
    [selectedScenario, createForm.scenarioName]
  );

  useEffect(() => {
    if (!simulationStatus.running) {
      return undefined;
    }
    const timerId = window.setInterval(() => {
      setSimulationElapsedSeconds(Math.max(0, Math.floor((Date.now() - (simulationStartedAtRef.current || Date.now())) / 1000)));
    }, 1000);
    return () => window.clearInterval(timerId);
  }, [simulationStatus.running]);

  useEffect(() => {
    if (!pendingDiceAircraft.length) {
      setSelectedAircraft('');
      return;
    }
    if (!pendingDiceAircraft.some((aircraft) => aircraft.code === selectedAircraft)) {
      setSelectedAircraft(pendingDiceAircraft[0].code);
    }
  }, [pendingDiceAircraft, selectedAircraft]);

  useEffect(() => {
    const roundPhase = gameState?.game?.roundPhase;
    if (autoDiceTimerRef.current) {
      window.clearTimeout(autoDiceTimerRef.current);
      autoDiceTimerRef.current = null;
    }
    if (!automationEnabled || !isValidGameId(gameId) || roundPhase !== 'DICE_ROLL' || !pendingDiceAircraft.length) {
      return undefined;
    }
    if (automationInFlightRef.current || status.kind === 'loading') {
      return undefined;
    }

    autoDiceTimerRef.current = window.setTimeout(async () => {
      autoDiceTimerRef.current = null;
      automationInFlightRef.current = true;
      try {
        const latestState = gameStateRef.current;
        const latestPendingDiceAircraft = (latestState?.aircraft || []).filter((aircraft) => aircraft.status === 'AWAITING_DICE_ROLL');
        const aircraftCode = latestPendingDiceAircraft[0]?.code;
        if (latestState?.game?.roundPhase !== 'DICE_ROLL') {
          return;
        }
        if (!aircraftCode) {
          return;
        }
        await submitDiceRoll(
          aircraftCode,
          automatedDiceValue(diceAutomation.strategy),
          true,
          automatedDiceSelectionMode(diceAutomation.strategy)
        );
      } finally {
        automationInFlightRef.current = false;
      }
    }, Number(diceAutomation.diceDelaySeconds || 0) * 1000);

    return () => {
      if (autoDiceTimerRef.current) {
        window.clearTimeout(autoDiceTimerRef.current);
        autoDiceTimerRef.current = null;
      }
    };
  }, [automationEnabled, diceAutomation.diceDelaySeconds, diceAutomation.strategy, gameId, gameState?.game?.roundPhase, pendingDiceAircraft, status.kind]);

  useEffect(() => {
    if (!automationEnabled || !canStartNextTurn || !isValidGameId(gameId) || status.kind === 'loading' || nextRoundInFlightRef.current) {
      setNextRoundCountdown(null);
      return undefined;
    }

    const initialDelay = Math.max(0, Number(diceAutomation.nextRoundDelaySeconds || 0));
    setNextRoundCountdown(initialDelay);

    if (initialDelay === 0) {
      void handleNextRound();
      return undefined;
    }

    const intervalId = window.setInterval(() => {
      setNextRoundCountdown((current) => {
        if (current === null) {
          return null;
        }
        if (current <= 1) {
          window.clearInterval(intervalId);
          void handleNextRound();
          return 0;
        }
        return current - 1;
      });
    }, 1000);

    return () => {
      window.clearInterval(intervalId);
      setNextRoundCountdown(null);
    };
  }, [automationEnabled, canStartNextTurn, diceAutomation.nextRoundDelaySeconds, gameId, status.kind]);

  const basesWithAircraft = useMemo(() => {
    const baseSource = gameState?.bases?.length
      ? gameState.bases
      : (selectedScenario?.bases || []).length
        ? (selectedScenario.bases || []).map((base) => ({
            code: base.code,
            name: BASE_TYPE_LABELS[base.baseTypeCode] || base.name,
            baseType: base.baseTypeCode || 'BASE',
            fuelStock: base.fuelStart || 0,
            weaponsStock: base.weaponsStart || 0,
            sparePartsStock: base.sparePartsStart || 0,
            occupiedParkingSlots: 0,
            parkingCapacity: base.parkingCapacity || 0,
            occupiedMaintSlots: 0,
            maintenanceCapacity: base.maintenanceCapacity || 0,
          }))
      : (rules?.bases || []).map((base) => ({
          code: base.code,
          name: base.name,
          baseType: 'BASE',
          fuelStock: base.startingInventory?.fuel || 0,
          weaponsStock: base.startingInventory?.weapons || 0,
          sparePartsStock: base.startingInventory?.spareParts || 0,
          occupiedParkingSlots: 0,
          parkingCapacity: base.parkingSlots || 0,
          occupiedMaintSlots: 0,
          maintenanceCapacity: base.maintenanceSlots || 0,
        }));

    return baseSource.map((base) => {
      const onBase = (gameState?.aircraft || []).filter((aircraft) => aircraft.currentBase === base.code);
      const parked = onBase.filter((aircraft) => ['READY', 'PARKED', 'WAITING_MAINTENANCE'].includes(aircraft.status));
      const maintenance = onBase.filter((aircraft) => aircraft.status === 'IN_MAINTENANCE');
      return { ...base, parked, maintenance };
    });
  }, [gameState, rules, selectedScenario]);

  const baseReferenceByCode = useMemo(() => {
    if ((selectedScenario?.bases || []).length) {
      return Object.fromEntries((selectedScenario.bases || []).map((base) => [normalizeBaseCode(base.code), {
        code: base.code,
        maxInventory: {
          fuel: base.fuelMax ?? 0,
          weapons: base.weaponsMax ?? 0,
          spareParts: base.sparePartsMax ?? 0,
        },
      }]));
    }
    return Object.fromEntries((rules?.bases || []).map((base) => [normalizeBaseCode(base.code), base]));
  }, [rules, selectedScenario]);

  const aircraftAdditionsByCode = useMemo(() => {
    const previousAircraft = Object.fromEntries((previousGameState?.aircraft || []).map((aircraft) => [aircraft.code, aircraft]));
    return Object.fromEntries((gameState?.aircraft || []).map((aircraft) => {
      const previous = previousAircraft[aircraft.code];
      return [aircraft.code, {
        fuel: Math.max(0, (aircraft.fuel ?? 0) - (previous?.fuel ?? aircraft.fuel ?? 0)),
        weapons: Math.max(0, (aircraft.weapons ?? 0) - (previous?.weapons ?? aircraft.weapons ?? 0)),
        hours: Math.max(0, (aircraft.remainingFlightHours ?? 0) - (previous?.remainingFlightHours ?? aircraft.remainingFlightHours ?? 0)),
      }];
    }));
  }, [gameState, previousGameState]);

  const missionCards = useMemo(() => {
    if (!rules?.missions) {
      return [];
    }
    const missionReferenceByCode = Object.fromEntries(rules.missions.map((mission) => [mission.code, mission]));

    if (gameState?.missions?.length) {
      return gameState.missions.map((runtime) => {
        const mission = missionReferenceByCode[runtime.missionType];
        return {
          code: runtime.code,
          name: mission?.name || runtime.missionType,
          flightHours: mission?.flightHours ?? 0,
          fuelCost: mission?.fuelCost ?? 0,
          weaponCost: mission?.weaponCost ?? 0,
          status: runtime.status || 'UNKNOWN',
          blocker: runtime.assignmentBlocker || null,
        };
      });
    }

    return rules.missions.flatMap((mission) =>
      Array.from({ length: Number(createForm.missionTypeCounts?.[mission.code] ?? 0) }, (_, index) => ({
        code: `${mission.code}-${index + 1}`,
        name: mission.name,
        flightHours: mission.flightHours,
        fuelCost: mission.fuelCost,
        weaponCost: mission.weaponCost,
        status: 'UNKNOWN',
        blocker: null,
      }))
    );
  }, [rules, gameState, createForm.missionTypeCounts]);

  const scenarioAircraftGroups = useMemo(() => {
    const grouped = new Map();
    for (const aircraft of selectedScenario?.aircraft || []) {
      const typeCode = aircraft.aircraftTypeCode || 'Unknown';
      if (!grouped.has(typeCode)) {
        grouped.set(typeCode, []);
      }
      grouped.get(typeCode).push(aircraft);
    }
    return Array.from(grouped.entries()).map(([typeCode, aircraft]) => ({
      typeCode,
      aircraft,
      representative: aircraft[0],
    }));
  }, [selectedScenario]);

  const totalScenarioParkingCapacity = useMemo(
    () => (selectedScenario?.bases || []).reduce((total, base) => total + Number(base.parkingCapacity || 0), 0),
    [selectedScenario]
  );

  const simulationMissionSummary = useMemo(
    () => (selectedScenario?.missions || []).map((mission) => ({
      code: mission.code,
      name: mission.missionTypeName || mission.missionTypeCode || mission.code,
      count: Number(simulationForm.missionTypeCounts?.[mission.code] || 0),
    })),
    [selectedScenario, simulationForm.missionTypeCounts]
  );

  const controlCenterPanel = (
    <section className="event-panel control-center-panel">
      <h3>Control center</h3>
      <form className="create-form" onSubmit={handleCreateGame}>
      <label>
        Scenario
        <select
          value={createForm.scenarioName}
          onChange={(event) => {
            const nextScenarioName = event.target.value;
            const nextScenario = scenarios.find((scenario) => scenario.name === nextScenarioName);
            if (nextScenario) {
              setSelectedScenarioId(String(nextScenario.scenarioId));
            }
            setCreateForm((current) => ({
              ...current,
              scenarioName: nextScenarioName,
            }));
          }}
        >
          {scenarios.map((scenario) => (
            <option key={scenario.scenarioId} value={scenario.name}>{scenario.name}</option>
          ))}
        </select>
      </label>
      <button type="button" className="play-action-button compact-button" onClick={() => setShowScenarioRules((current) => !current)}>
        {showScenarioRules ? 'Hide scenario rules' : 'Show scenario rules'}
      </button>
      {showScenarioRules ? (
        <article className="scenario-rules-panel">
          <h4>{selectedScenarioRules.title}</h4>
          <p className="muted-copy">{selectedScenarioRules.summary}</p>
          <ul className="compact-list scenario-rules-list">
            {selectedScenarioRules.points.map((point) => (
              <li key={point}>{point}</li>
            ))}
          </ul>
        </article>
      ) : null}
      <fieldset className="radio-group">
        <legend>Dice handling</legend>
        <label className="radio-option">
          <input
            type="radio"
            name="diceAutomationMode"
            checked={diceAutomation.mode === 'MANUAL'}
            onChange={() => setDiceAutomation((current) => ({ ...current, mode: 'MANUAL' }))}
          />
          <span>Manual</span>
        </label>
        <label className="radio-option">
          <input
            type="radio"
            name="diceAutomationMode"
            checked={diceAutomation.mode === 'AUTOMATED'}
            onChange={() => setDiceAutomation((current) => ({ ...current, mode: 'AUTOMATED' }))}
          />
          <span>Automated</span>
        </label>
      </fieldset>
      {diceAutomation.mode === 'AUTOMATED' ? (
        <article className="scenario-rules-panel automation-panel">
          <label>
            Dice strategy
            <select
              value={diceAutomation.strategy}
              onChange={(event) => setDiceAutomation((current) => ({ ...current, strategy: event.target.value }))}
            >
              <option value="RANDOM">Random dice outcome</option>
              <option value="MIN_DAMAGE">Favor as little damage as possible</option>
              <option value="MAX_DAMAGE">Cause as much damage as possible</option>
            </select>
          </label>
          <label>
            Mission preview seconds
            <input
              type="number"
              min="0"
              value={diceAutomation.missionPreviewSeconds}
              onChange={(event) => setDiceAutomation((current) => ({
                ...current,
                missionPreviewSeconds: Math.max(0, Number(event.target.value) || 0),
              }))}
            />
          </label>
          <label>
            Seconds before dice roll
            <input
              type="number"
              min="0"
              value={diceAutomation.diceDelaySeconds}
              onChange={(event) => setDiceAutomation((current) => ({
                ...current,
                diceDelaySeconds: Math.max(0, Number(event.target.value) || 0),
              }))}
            />
          </label>
          <label>
            Seconds before next round
            <input
              type="number"
              min="0"
              value={diceAutomation.nextRoundDelaySeconds}
              onChange={(event) => setDiceAutomation((current) => ({
                ...current,
                nextRoundDelaySeconds: Math.max(0, Number(event.target.value) || 0),
              }))}
            />
          </label>
          <p className="muted-copy">Automated dice rolls and next rounds use separate wait times.</p>
        </article>
      ) : null}
      <label>
        Max rounds
        <input
          type="number"
          min="1"
          value={createForm.maxRounds}
          onChange={(event) => setCreateForm((current) => ({
            ...current,
            maxRounds: Math.max(1, Number(event.target.value) || 1000),
          }))}
        />
      </label>
      <span className="field-warning-copy">Upper limit for how many rounds this game may take before it is marked as lost.</span>
      <label>
        Aircraft
        <input
          type="number"
          min="1"
          max="8"
          value={createForm.aircraftCount}
          onChange={(event) => setCreateForm((current) => ({
            ...current,
            aircraftCount: Math.min(8, Math.max(1, Number(event.target.value) || 1)),
          }))}
        />
      </label>
      <p className="muted-copy">Max 8 aircraft in the current scenario.</p>
        {rules?.missions?.map((mission) => (
          <label key={mission.code}>
            {mission.code} missions
            <input
              type="number"
              min="0"
              value={createForm.missionTypeCounts?.[mission.code] ?? 0}
              onChange={(event) => setCreateForm((current) => ({
                ...current,
                missionTypeCounts: {
                  ...current.missionTypeCounts,
                  [mission.code]: Number(event.target.value),
                },
              }))}
            />
          </label>
        ))}
        <button type="submit" className={`play-action-button compact-button ${nextStep === 'CREATE_GAME' ? 'next-step-button' : ''}`.trim()} disabled={hasOngoingGame}>Create game</button>
        {showCreateGamePrompt ? (
          <article className="scenario-rules-panel">
            <h4>Name game</h4>
            <p className="muted-copy">Choose the default game name with a running number, or enter your own name.</p>
            <div className="button-row">
              <button type="button" onClick={handleCreateGameWithDefaultName} disabled={status.kind === 'loading'}>
                Use default name
              </button>
              <button
                type="button"
                className={useCustomGameName ? 'next-step-button' : ''}
                onClick={() => setUseCustomGameName(true)}
                disabled={status.kind === 'loading'}
              >
                Enter name
              </button>
            </div>
            {useCustomGameName ? (
              <>
                <label>
                  Game name
                  <input
                    value={createForm.gameName}
                    onChange={(event) => setCreateForm((current) => ({
                      ...current,
                      gameName: event.target.value,
                    }))}
                    aria-invalid={!createForm.gameName.trim()}
                    disabled={status.kind === 'loading'}
                  />
                </label>
                {!createForm.gameName.trim() ? (
                  <p className="muted-copy">Game name is required when you choose a custom name.</p>
                ) : null}
                <div className="button-row">
                  <button
                    type="button"
                    onClick={handleCreateGameWithCustomName}
                    disabled={status.kind === 'loading' || !createForm.gameName.trim()}
                  >
                    Create named game
                  </button>
                </div>
              </>
            ) : null}
          </article>
        ) : null}
      </form>
      <label>
        Game ID
        <input value={gameId} onChange={(event) => setGameId(event.target.value)} placeholder="Game id" />
      </label>
      <label>
        Current game name
        <input value={gameState?.game?.name || 'No active game'} readOnly aria-label="Current game name" />
      </label>
      <div className="button-row play-control-row">
        <button type="button" className={`play-action-button compact-button ${nextStep === 'NEXT_TURN' ? 'next-step-button' : ''}`.trim()} onClick={handleNextRound} disabled={!canStartNextTurn}>Next turn</button>
        <button type="button" className={`play-action-button compact-button ${nextStep === 'RESOLVE_MISSIONS' ? 'next-step-button' : ''}`.trim()} onClick={handleResolveMissions} disabled={!canResolveMissions}>Resolve missions</button>
        <button
          type="button"
          className={`play-action-button compact-button ${hasOngoingGame ? 'active-button' : ''}`.trim()}
          onClick={handleResetView}
          disabled={!hasOngoingGame}
        >
          Abort game
        </button>
      </div>
      {automationEnabled && nextRoundCountdown !== null ? (
        <p className="muted-copy">Auto next round in {nextRoundCountdown}s. You can still press Next turn manually.</p>
      ) : null}
      <p className={`status-pill status-${status.kind}`}>{status.message}</p>
    </section>
  );

async function request(path, options = {}) {
    const controller = new AbortController();
    activeRequestControllersRef.current.add(controller);
    try {
      const response = await fetch(`${API_BASE_URL}${path}`, {
        headers: {
          'Content-Type': 'application/json',
        },
        ...options,
        signal: controller.signal,
      });

      const text = await response.text();
      const data = text ? JSON.parse(text) : null;

      if (!response.ok) {
        throw new Error(normalizeApiErrorMessage(data?.message || text || `Request failed with ${response.status}`));
      }

      return data;
    } finally {
      activeRequestControllersRef.current.delete(controller);
    }
  }

  function normalizeApiErrorMessage(message) {
    const normalized = String(message || '').trim();
    if (normalized.startsWith('Error calling tool:')) {
      const match = normalized.match(TOOL_WRAPPER_MESSAGE_PATTERN);
      if (match?.[1]) {
        return match[1].trim();
      }
    }
    return normalized;
  }

  function isAbortError(error) {
    return error?.name === 'AbortError';
  }

  useEffect(() => {
    let ignore = false;

    async function loadInitialData() {
      try {
        const [rulesData, scenarioData] = await Promise.all([
          request('/reference/rules'),
          request('/scenarios'),
        ]);
        if (!ignore) {
          setRules(rulesData);
          setScenarios(scenarioData || []);
          if ((scenarioData || []).length) {
            const initialScenarioId = String(scenarioData[0].scenarioId);
            setSelectedScenarioId(initialScenarioId);
          }
        }
      } catch (error) {
        if (isAbortError(error)) {
          return;
        }
        if (!ignore) {
          setStatus({ kind: 'error', message: error.message });
        }
      }
    }

    loadInitialData();
    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => {
    if (!selectedScenarioId) {
      setSelectedScenario(null);
      return;
    }

    let ignore = false;

    async function loadScenario() {
      try {
        const data = await request(`/scenarios/${selectedScenarioId}`);
        if (!ignore) {
          setSelectedScenario(data);
          setCreateForm((current) => ({
            ...current,
            scenarioName: data?.name || current.scenarioName,
            aircraftCount: (data?.aircraft || []).length || current.aircraftCount,
            missionTypeCounts: Object.fromEntries((data?.missions || []).map((mission) => [
              mission.missionTypeCode || mission.code,
              Number(mission.defaultCount ?? 0),
            ])),
          }));
          setSimulationForm((current) => ({
            ...current,
            scenarioName: data?.name || current.scenarioName,
            aircraftCount: (data?.aircraft || []).length || current.aircraftCount,
            missionTypeCounts: {
              M1: 0,
              M2: 0,
              M3: 0,
              ...Object.fromEntries((data?.missions || []).map((mission) => [
                mission.missionTypeCode || mission.code,
                Number(mission.defaultCount ?? 0),
              ])),
            },
            batchName: current.batchName || defaultSimulationBatchName(data?.name),
          }));
          setDuplicateScenarioName(defaultDuplicateScenarioName(data?.name));
        }
      } catch (error) {
        if (isAbortError(error)) {
          return;
        }
        if (!ignore) {
          setStatus({ kind: 'error', message: error.message });
        }
      }
    }

    void loadScenario();
    return () => {
      ignore = true;
    };
  }, [selectedScenarioId]);

  useEffect(() => {
    if (currentView !== 'DASHBOARD') {
      return;
    }

    let ignore = false;

    async function loadDashboardRows() {
      setDashboardLoading(true);
      try {
        const data = await request('/analytics/games');
        if (!ignore) {
          setDashboardAllRows(data || []);
        }
      } catch (error) {
        if (isAbortError(error)) {
          return;
        }
        if (!ignore) {
          setStatus({ kind: 'error', message: error.message });
        }
      } finally {
        if (!ignore) {
          setDashboardLoading(false);
        }
      }
    }

    loadDashboardRows();
    return () => {
      ignore = true;
    };
  }, [currentView]);

  const dashboardFilterOptions = useMemo(() => ({
    scenarios: Array.from(new Set((dashboardAllRows || []).map((row) => row.scenarioName).filter(Boolean))),
    dates: Array.from(new Set((dashboardAllRows || []).map((row) => String(row.createdAt || '').slice(0, 10)).filter(Boolean))),
    aircraftCounts: Array.from(new Set((dashboardAllRows || []).map((row) => row.aircraftCount).filter((value) => value !== null && value !== undefined))).sort((a, b) => a - b),
    m1Counts: Array.from(new Set((dashboardAllRows || []).map((row) => row.m1Count).filter((value) => value !== null && value !== undefined))).sort((a, b) => a - b),
    m2Counts: Array.from(new Set((dashboardAllRows || []).map((row) => row.m2Count).filter((value) => value !== null && value !== undefined))).sort((a, b) => a - b),
    m3Counts: Array.from(new Set((dashboardAllRows || []).map((row) => row.m3Count).filter((value) => value !== null && value !== undefined))).sort((a, b) => a - b),
  }), [dashboardAllRows]);

  const dashboardVisibleRows = useMemo(
    () => (dashboardAllRows || []).filter((row) => {
      if (dashboardFilters.scenarioName && row.scenarioName !== dashboardFilters.scenarioName) {
        return false;
      }
      if (dashboardFilters.createdDate && String(row.createdAt || '').slice(0, 10) !== dashboardFilters.createdDate) {
        return false;
      }
      if (dashboardFilters.aircraftCount !== '' && Number(row.aircraftCount) !== Number(dashboardFilters.aircraftCount)) {
        return false;
      }
      if (dashboardFilters.m1Count !== '' && Number(row.m1Count) !== Number(dashboardFilters.m1Count)) {
        return false;
      }
      if (dashboardFilters.m2Count !== '' && Number(row.m2Count) !== Number(dashboardFilters.m2Count)) {
        return false;
      }
      if (dashboardFilters.m3Count !== '' && Number(row.m3Count) !== Number(dashboardFilters.m3Count)) {
        return false;
      }
      return true;
    }),
    [dashboardAllRows, dashboardFilters]
  );

  const dashboardTotalRows = dashboardVisibleRows.length;
  const dashboardTotalPages = Math.max(1, Math.ceil(dashboardTotalRows / DASHBOARD_PAGE_SIZE));
  const dashboardPagedRows = useMemo(() => {
    const startIndex = (dashboardPage - 1) * DASHBOARD_PAGE_SIZE;
    return dashboardVisibleRows.slice(startIndex, startIndex + DASHBOARD_PAGE_SIZE);
  }, [dashboardPage, dashboardVisibleRows]);

  useEffect(() => {
    setDashboardPage(1);
  }, [dashboardFilters, dashboardAllRows]);

  useEffect(() => {
    if (dashboardPage > dashboardTotalPages) {
      setDashboardPage(dashboardTotalPages);
    }
  }, [dashboardPage, dashboardTotalPages]);

  async function handleExportDashboardCsv() {
    const normalizedFileName = normalizeDashboardExportFileName(dashboardExportFileName);
    if (!normalizedFileName) {
      setDashboardExportStatus({ kind: 'error', message: 'Enter a file name before exporting.' });
      return;
    }
    if (!dashboardVisibleRows.length) {
      setDashboardExportStatus({ kind: 'error', message: 'There are no rows to export.' });
      return;
    }

    const csvContent = buildDashboardCsv(dashboardVisibleRows);
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const finalFileName = `${normalizedFileName}.csv`;

    try {
      if (typeof window.showSaveFilePicker === 'function') {
        const handle = await window.showSaveFilePicker({
          suggestedName: finalFileName,
          types: [
            {
              description: 'CSV file',
              accept: { 'text/csv': ['.csv'] },
            },
          ],
        });
        const writable = await handle.createWritable();
        await writable.write(blob);
        await writable.close();
      } else {
        const objectUrl = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = objectUrl;
        link.download = finalFileName;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(objectUrl);
      }
      setDashboardExportStatus({ kind: 'success', message: `Exported ${dashboardVisibleRows.length} rows to ${finalFileName}.` });
      setDashboardExportFileName(normalizedFileName);
    } catch (error) {
      if (error?.name === 'AbortError') {
        setDashboardExportStatus({ kind: 'idle', message: '' });
        return;
      }
      setDashboardExportStatus({ kind: 'error', message: error?.message || 'Failed to export dashboard rows.' });
    }
  }

  useEffect(() => {
    return () => {
      if (missionPreviewTimerRef.current) {
        window.clearTimeout(missionPreviewTimerRef.current);
      }
      if (autoDiceTimerRef.current) {
        window.clearTimeout(autoDiceTimerRef.current);
      }
    };
  }, []);

  useEffect(() => {
    const round = gameState?.game?.currentRound;
    const statusValue = gameState?.game?.status;
    const shouldGenerate = isValidGameId(gameId)
      && round > 0
      && !gameState?.game?.roundOpen
      && statusValue === 'ACTIVE';
    if (!shouldGenerate) {
      return undefined;
    }

    const requestKey = `${gameId}-${round}-${statusValue}`;
    if (lastAnalysisRequestKeyRef.current === requestKey) {
      return undefined;
    }

    let ignore = false;
    lastAnalysisRequestKeyRef.current = requestKey;
    setAnalysisPending(true);

    async function generateAnalysis() {
      try {
        await request(`/games/${gameId}/analysis/generate`, { method: 'POST' });
        const feedResponse = await request(`/games/${gameId}/analysis-feed`);
        if (!ignore) {
          setAnalysisFeed(feedResponse.items || []);
        }
      } catch (error) {
        if (isAbortError(error)) {
          return;
        }
        if (!ignore) {
          setAnalysisFeed((current) => current);
        }
      } finally {
        if (!ignore) {
          setAnalysisPending(false);
        }
      }
    }

    void generateAnalysis();
    return () => {
      ignore = true;
    };
  }, [gameId, gameState]);

  useEffect(() => {
    const statusValue = gameState?.game?.status;
    if (!isValidGameId(gameId) || !['WON', 'LOST'].includes(statusValue)) {
      return undefined;
    }
    const requestKey = `${gameId}-final-${statusValue}`;
    if (lastFinalAnalysisRequestKeyRef.current === requestKey) {
      return undefined;
    }
    let ignore = false;
    lastFinalAnalysisRequestKeyRef.current = requestKey;

    async function generateFinalAnalysis() {
      try {
        const summaryResponse = await request(`/games/${gameId}/analysis/generate-final`, { method: 'POST' });
        if (!ignore) {
          setPostGameSummary(summaryResponse);
          const feedResponse = await request(`/games/${gameId}/analysis-feed`);
          if (!ignore) {
            setAnalysisFeed(feedResponse.items || []);
          }
        }
      } catch (error) {
        if (isAbortError(error)) {
          return;
        }
      }
    }

    void generateFinalAnalysis();
    return () => {
      ignore = true;
    };
  }, [gameId, gameState]);

  useEffect(() => {
    const container = analysisFeedListRef.current;
    if (!container) {
      return;
    }
    container.scrollTop = container.scrollHeight;
  }, [analysisFeed, analysisPending]);

  function pushLog(title, payload) {
    const stamp = new Date().toLocaleString('sv-SE', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    });
    setEventLog((current) => [
      {
        id: `${Date.now()}-${Math.random()}`,
        stamp,
        title,
        payload,
      },
      ...current,
    ]);
  }

  async function refreshGameState(targetGameId = gameId) {
    if (!isValidGameId(targetGameId)) {
      return;
    }
    const data = await request(`/games/${targetGameId}`);
    applyGameState(data);
    return data;
  }

  function applyGameState(nextState) {
    if (['WON', 'LOST', 'ABORTED'].includes(nextState?.game?.status)) {
      stopAutomation();
    }
    gameStateRef.current = nextState;
    setPreviousGameState(gameState);
    setGameState(nextState);
    if (['WON', 'LOST', 'ABORTED'].includes(nextState?.game?.status)) {
      setAnalysisPending(false);
    }
  }

  function clearMissionPreview() {
    if (missionPreviewTimerRef.current) {
      window.clearTimeout(missionPreviewTimerRef.current);
      missionPreviewTimerRef.current = null;
    }
    setMissionPreviewActive(false);
  }

  function stopAutomation() {
    automationInFlightRef.current = false;
    nextRoundInFlightRef.current = false;
    setNextRoundCountdown(null);
    setManualMissionPreviewAssignments({});
    if (autoDiceTimerRef.current) {
      window.clearTimeout(autoDiceTimerRef.current);
      autoDiceTimerRef.current = null;
    }
    clearMissionPreview();
  }

  function abortActiveRequests() {
    activeRequestControllersRef.current.forEach((controller) => controller.abort());
    activeRequestControllersRef.current.clear();
  }

  function prepareForNewGameCreation() {
    stopAutomation();
    abortActiveRequests();
    setGameId('');
    setShowScenarioRules(false);
    setShowCreateGamePrompt(false);
    setUseCustomGameName(false);
    setPreviousGameState(null);
    gameStateRef.current = null;
    setGameState(null);
    setLastAutoResponse(null);
    setSelectedAircraft('');
    setDiceValue(1);
    setUseRandomDice(true);
    setStatus({ kind: 'idle', message: 'Create a game to begin.' });
    setEventLog([]);
    setAnalysisFeed([]);
    setAnalysisPending(false);
    setPostGameSummary(null);
    lastAnalysisRequestKeyRef.current = null;
    lastFinalAnalysisRequestKeyRef.current = null;
  }

  async function handleCreateGame(event) {
    event.preventDefault();
    // Starting a fresh create flow should clear prior transient UI state so the
    // naming prompt and event feed always begin from the startup baseline.
    prepareForNewGameCreation();
    setShowCreateGamePrompt(true);
    setUseCustomGameName(false);
    setCreateForm((current) => ({ ...current, gameName: '' }));
  }

  async function finalizeCreatedGame(data, successPrefix) {
    if (!isValidGameId(data?.gameId)) {
      throw new Error('Create game did not return a valid gameId.');
    }
    const nextGameId = String(data.gameId);
    const createdGameName = data?.name || `Game ${nextGameId}`;
    setGameId(nextGameId);
    setLastAutoResponse(null);
    setSelectedAircraft('');
    setDiceValue(1);
    setUseRandomDice(true);
    setEventLog([]);
    setAnalysisFeed([]);
    setAnalysisPending(false);
    setPostGameSummary(null);
    lastAnalysisRequestKeyRef.current = null;
    lastFinalAnalysisRequestKeyRef.current = null;
    setPreviousGameState(null);
    stopAutomation();
    await refreshGameState(nextGameId);
    setCurrentView('PLAY');
    setShowCreateGamePrompt(false);
    setUseCustomGameName(false);
    setCreateForm((current) => ({ ...current, gameName: '' }));
    setStatus({ kind: 'success', message: `${successPrefix} ${createdGameName} (${nextGameId}).` });
    pushLog(successPrefix, {
      ...data,
      messages: [`${createdGameName} created with Game ID ${nextGameId}.`],
    });
  }

  function resetUiToStartupState(previousGameId, previousGameStatus) {
    stopAutomation();
    abortActiveRequests();
    setCreateForm(INITIAL_CREATE_FORM);
    setDiceAutomation(INITIAL_DICE_AUTOMATION);
    setGameId('');
    setRules((current) => current);
    setShowScenarioRules(false);
    setShowCreateGamePrompt(false);
    setUseCustomGameName(false);
    setPreviousGameState(null);
    gameStateRef.current = null;
    setGameState(null);
    setLastAutoResponse(null);
    setSelectedAircraft('');
    setDiceValue(1);
    setUseRandomDice(true);
    setStatus({ kind: 'idle', message: 'Create a game to begin.' });
    setEventLog([]);
    setAnalysisFeed([]);
    setAnalysisPending(false);
    setPostGameSummary(null);
    lastAnalysisRequestKeyRef.current = null;
    lastFinalAnalysisRequestKeyRef.current = null;

    if (previousGameId) {
      pushLog('Game aborted', {
        messages: [
          previousGameStatus
            ? `Game ${previousGameId} ended through abort with status ${previousGameStatus}.`
            : `Game ${previousGameId} ended through abort.`,
        ],
      });
    }
  }

  async function handleAbortGame() {
    const previousGameId = isValidGameId(gameId) ? String(gameId) : null;
    if (!previousGameId) {
      resetUiToStartupState(null, null);
      return;
    }

    stopAutomation();
    setStatus({ kind: 'loading', message: 'Aborting game...' });
    try {
      await request(`/games/${previousGameId}/abort`, { method: 'POST' });
      resetUiToStartupState(previousGameId, 'ABORTED');
    } catch (error) {
      if (isAbortError(error)) {
        return;
      }
      setStatus({ kind: 'error', message: error.message });
    }
  }

  async function handleStartSimulation(event) {
    event.preventDefault();
    if (workspaceLocked) {
      return;
    }
    if (!simulationForm.batchName.trim()) {
      setSimulationStatus((current) => ({
        ...current,
        message: 'Simulation batch name is required.',
      }));
      return;
    }

    const config = {
      batchName: normalizeScenarioTemplateName(simulationForm.batchName),
      scenarioName: simulationForm.scenarioName,
      aircraftCount: Math.max(1, Number(simulationForm.aircraftCount || 1)),
      missionTypeCounts: {
        M1: Math.max(0, Number(simulationForm.missionTypeCounts?.M1 || 0)),
        M2: Math.max(0, Number(simulationForm.missionTypeCounts?.M2 || 0)),
        M3: Math.max(0, Number(simulationForm.missionTypeCounts?.M3 || 0)),
      },
      runCount: Math.max(1, Number(simulationForm.runCount || 1)),
      maxRounds: Math.max(1, Number(simulationForm.maxRounds || 1000)),
      diceStrategy: simulationForm.diceStrategy,
    };

    setCurrentView('SIMULATOR');
    simulationStartedAtRef.current = Date.now();
    setSimulationElapsedSeconds(0);
    setSimulationStatus({
      running: true,
      completedRuns: 0,
      failedRuns: 0,
      wonRuns: 0,
      lostRuns: 0,
      requestedRuns: config.runCount,
      currentGameName: '',
      batchName: config.batchName,
      scenarioName: config.scenarioName,
      aircraftCount: config.aircraftCount,
      missionTypeCounts: config.missionTypeCounts,
      diceStrategy: config.diceStrategy,
      maxRounds: config.maxRounds,
      message: `Running simulation batch ${config.batchName}...`,
    });

    try {
      const batch = await request('/simulations', {
        method: 'POST',
        body: JSON.stringify(config),
      });

      let latestBatch = batch;
      while (latestBatch?.status === 'PENDING' || latestBatch?.status === 'RUNNING') {
        setSimulationStatus({
          running: true,
          completedRuns: latestBatch.completedRuns || 0,
          failedRuns: latestBatch.failedRuns || 0,
          wonRuns: latestBatch.wonRuns || 0,
          lostRuns: latestBatch.lostRuns || 0,
          requestedRuns: latestBatch.requestedRuns || config.runCount,
          currentGameName: latestBatch.currentGameName || '',
          batchName: latestBatch.name || config.batchName,
          scenarioName: latestBatch.scenarioName || config.scenarioName,
          aircraftCount: latestBatch.aircraftCount || config.aircraftCount,
          missionTypeCounts: {
            M1: latestBatch.m1Count || 0,
            M2: latestBatch.m2Count || 0,
            M3: latestBatch.m3Count || 0,
          },
          diceStrategy: latestBatch.diceStrategy || config.diceStrategy,
          maxRounds: latestBatch.maxRounds || config.maxRounds,
          message: latestBatch.currentGameName
            ? `Running ${latestBatch.currentGameName} (${latestBatch.completedRuns || 0}/${latestBatch.requestedRuns || config.runCount} completed)...`
            : `Running simulation batch ${latestBatch.name || config.batchName}...`,
        });
        await sleep(SIMULATION_POLL_INTERVAL_MS);
        latestBatch = await request(`/simulations/${batch.simulationBatchId}`);
      }

      const completedRuns = latestBatch?.completedRuns || 0;
      const failedRuns = latestBatch?.failedRuns || 0;
      const requestedRuns = latestBatch?.requestedRuns || config.runCount;
      setSimulationStatus({
        running: false,
        completedRuns,
        failedRuns,
        wonRuns: latestBatch?.wonRuns || 0,
        lostRuns: latestBatch?.lostRuns || 0,
        requestedRuns,
        currentGameName: '',
        batchName: latestBatch?.name || config.batchName,
        scenarioName: latestBatch?.scenarioName || config.scenarioName,
        aircraftCount: latestBatch?.aircraftCount || config.aircraftCount,
        missionTypeCounts: {
          M1: latestBatch?.m1Count || 0,
          M2: latestBatch?.m2Count || 0,
          M3: latestBatch?.m3Count || 0,
        },
        diceStrategy: latestBatch?.diceStrategy || config.diceStrategy,
        maxRounds: latestBatch?.maxRounds || config.maxRounds,
        message: latestBatch?.status === 'FAILED'
          ? `Simulation batch failed after ${completedRuns}/${requestedRuns} completed${failedRuns ? `, ${failedRuns} failed` : ''}.`
          : `Simulation batch completed. ${completedRuns}/${requestedRuns} games saved${failedRuns ? `, ${failedRuns} failed` : ''}.`,
      });
    } catch (error) {
      if (isAbortError(error)) {
        setSimulationStatus({
          running: false,
          completedRuns: 0,
          failedRuns: 0,
          wonRuns: 0,
          lostRuns: 0,
          requestedRuns: config.runCount,
          currentGameName: '',
          batchName: config.batchName,
          scenarioName: config.scenarioName,
          aircraftCount: config.aircraftCount,
          missionTypeCounts: config.missionTypeCounts,
          diceStrategy: config.diceStrategy,
          maxRounds: config.maxRounds,
          message: 'Simulation batch was interrupted.',
        });
        return;
      }
      setSimulationStatus({
        running: false,
        completedRuns: 0,
        failedRuns: 0,
        wonRuns: 0,
        lostRuns: 0,
        requestedRuns: config.runCount,
        currentGameName: '',
        batchName: config.batchName,
        scenarioName: config.scenarioName,
        aircraftCount: config.aircraftCount,
        missionTypeCounts: config.missionTypeCounts,
        diceStrategy: config.diceStrategy,
        maxRounds: config.maxRounds,
        message: error.message,
      });
    }
  }

  function handleResetView() {
    void handleAbortGame();
  }

  async function createNewGame(loadingMessage, successPrefix, options = {}) {
    if (!selectedScenarioId) {
      setStatus({ kind: 'error', message: 'Select a scenario before creating a game.' });
      return;
    }
    setStatus({ kind: 'loading', message: loadingMessage });
    try {
      const data = await request(`/scenarios/${selectedScenarioId}/create-game`, {
        method: 'POST',
        body: JSON.stringify({
          gameName: options.gameName ?? null,
          aircraftCount: Math.max(1, Number(createForm.aircraftCount || 1)),
          missionTypeCounts: {
            M1: Math.max(0, Number(createForm.missionTypeCounts?.M1 || 0)),
            M2: Math.max(0, Number(createForm.missionTypeCounts?.M2 || 0)),
            M3: Math.max(0, Number(createForm.missionTypeCounts?.M3 || 0)),
          },
          maxRounds: Math.max(1, Number(createForm.maxRounds || 1000)),
        }),
      });
      await finalizeCreatedGame(data, successPrefix);
    } catch (error) {
      if (isAbortError(error)) {
        return;
      }
      setStatus({ kind: 'error', message: error.message });
    }
  }

  async function handleCreateGameWithDefaultName() {
    await createNewGame('Creating game...', 'Game created');
  }

  async function handleCreateGameWithCustomName() {
    if (!createForm.gameName.trim()) {
      setStatus({ kind: 'error', message: 'Enter a name for the game or use the default name.' });
      return;
    }
    await createNewGame('Creating game...', 'Game created', { gameName: createForm.gameName.trim() });
  }

  async function handleDuplicateScenario() {
    if (!selectedScenarioId || !duplicateScenarioName.trim()) {
      setStatus({ kind: 'error', message: 'Enter a name for the duplicated scenario.' });
      return;
    }
    setScenarioBusy(true);
    setStatus({ kind: 'loading', message: 'Duplicating scenario...' });
    try {
      const duplicated = await request(`/scenarios/${selectedScenarioId}/duplicate`, {
        method: 'POST',
        body: JSON.stringify({ name: duplicateScenarioName.trim() }),
      });
      const updatedScenarios = await request('/scenarios');
      setScenarios(updatedScenarios || []);
      setSelectedScenarioId(String(duplicated.scenarioId));
      setStatus({ kind: 'success', message: `Scenario duplicated as ${duplicated.name}.` });
    } catch (error) {
      if (isAbortError(error)) {
        return;
      }
      setStatus({ kind: 'error', message: error.message });
    } finally {
      setScenarioBusy(false);
    }
  }

  async function handleDeleteScenario() {
    if (!selectedScenarioId || !selectedScenario?.deletable) {
      setStatus({ kind: 'error', message: 'This scenario cannot be deleted.' });
      return;
    }
    setScenarioBusy(true);
    setStatus({ kind: 'loading', message: `Deleting ${selectedScenario.name}...` });
    try {
      await request(`/scenarios/${selectedScenarioId}`, { method: 'DELETE' });
      const updatedScenarios = await request('/scenarios');
      setScenarios(updatedScenarios || []);
      const nextScenario = (updatedScenarios || [])[0] || null;
      setSelectedScenarioId(nextScenario ? String(nextScenario.scenarioId) : '');
      setSelectedScenario(null);
      setDuplicateScenarioName(nextScenario ? defaultDuplicateScenarioName(nextScenario.name) : '');
      setStatus({ kind: 'success', message: 'Scenario deleted.' });
    } catch (error) {
      if (isAbortError(error)) {
        return;
      }
      setStatus({ kind: 'error', message: error.message });
    } finally {
      setScenarioBusy(false);
    }
  }

  function updateSelectedScenarioCollection(section, itemCode, patch) {
    setSelectedScenario((current) => {
      if (!current) {
        return current;
      }
      return {
        ...current,
        [section]: (current[section] || []).map((item) => (
          item.code === itemCode ? { ...item, ...patch } : item
        )),
      };
    });
  }

  function updateScenarioNumberField(section, itemCode, field, value) {
    const parsedValue = Number(value);
    const numericValue = Number.isFinite(parsedValue) ? Math.max(0, parsedValue) : 0;
    updateSelectedScenarioCollection(section, itemCode, { [field]: numericValue });
  }

  function updateScenarioTextField(section, itemCode, field, value) {
    updateSelectedScenarioCollection(section, itemCode, { [field]: value });
  }

  function updateScenarioSupplyRuleNumberField(baseCode, resource, value) {
    const parsedValue = Number(value);
    const numericValue = Number.isFinite(parsedValue) ? Math.max(0, parsedValue) : 0;
    setSelectedScenario((current) => {
      if (!current) {
        return current;
      }
      return {
        ...current,
        bases: (current.bases || []).map((base) => (
          base.code !== baseCode ? base : {
            ...base,
            supplyRules: (base.supplyRules || []).map((rule) => (
              rule.resource === resource ? { ...rule, deliveryAmount: numericValue } : rule
            )),
          }
        )),
      };
    });
  }

  function updateScenarioAircraftGroupField(typeCode, field, value) {
    setSelectedScenario((current) => {
      if (!current) {
        return current;
      }
      return {
        ...current,
        aircraft: (current.aircraft || []).map((aircraft) => (
          aircraft.aircraftTypeCode === typeCode ? { ...aircraft, [field]: value } : aircraft
        )),
      };
    });
  }

  function updateScenarioAircraftGroupNumberField(typeCode, field, value) {
    updateScenarioAircraftGroupField(typeCode, field, Math.max(0, Number(value || 0)));
  }

  function nextScenarioAircraftCode(aircraft) {
    const maxSequence = (aircraft || []).reduce((max, entry) => {
      const match = String(entry.code || '').match(/^F(\d+)$/i);
      return match ? Math.max(max, Number(match[1])) : max;
    }, 0);
    return `F${maxSequence + 1}`;
  }

  function updateScenarioAircraftGroupCount(typeCode, value) {
    setSelectedScenario((current) => {
      if (!current) {
        return current;
      }

      const aircraft = current.aircraft || [];
      const groupAircraft = aircraft.filter((entry) => entry.aircraftTypeCode === typeCode);
      if (!groupAircraft.length) {
        return current;
      }

      const parsedValue = Number(value);
      const otherAircraftCount = aircraft.length - groupAircraft.length;
      const maxAllowedCount = Math.max(1, totalScenarioParkingCapacity - otherAircraftCount);
      const desiredCount = Math.max(1, Math.min(maxAllowedCount, Number.isFinite(parsedValue) ? parsedValue : groupAircraft.length));

      if (desiredCount === groupAircraft.length) {
        return current;
      }

      // The editor persists one scenario-aircraft row per starting aircraft.
      // Reducing the count therefore removes the highest-numbered rows for that type.
      if (desiredCount < groupAircraft.length) {
        const removableCodes = new Set(
          [...groupAircraft]
            .sort((left, right) => String(right.code || '').localeCompare(String(left.code || '')))
            .slice(0, groupAircraft.length - desiredCount)
            .map((entry) => entry.code)
        );
        return {
          ...current,
          aircraft: aircraft.filter((entry) => !removableCodes.has(entry.code)),
        };
      }

      // Increasing the count clones the representative setup for the type and
      // assigns the next available F-number in the scenario.
      const additions = [];
      let aircraftWithAdditions = [...aircraft];
      while (groupAircraft.length + additions.length < desiredCount) {
        const template = groupAircraft[0];
        const newAircraft = {
          ...template,
          code: nextScenarioAircraftCode(aircraftWithAdditions),
        };
        additions.push(newAircraft);
        aircraftWithAdditions = [...aircraftWithAdditions, newAircraft];
      }

      return {
        ...current,
        aircraft: [...aircraft, ...additions],
      };
    });
  }

  async function handleSaveScenario() {
    if (!selectedScenarioId || !selectedScenario?.editable) {
      setStatus({ kind: 'error', message: 'This scenario cannot be edited.' });
      return;
    }
    setScenarioBusy(true);
    setStatus({ kind: 'loading', message: `Saving ${selectedScenario.name}...` });
    try {
      const updatedScenario = await request(`/scenarios/${selectedScenarioId}`, {
        method: 'PUT',
        body: JSON.stringify({
          description: selectedScenario.description || '',
          bases: selectedScenario.bases || [],
          aircraft: selectedScenario.aircraft || [],
          missions: selectedScenario.missions || [],
        }),
      });
      setSelectedScenario(updatedScenario);
      setCreateForm((current) => ({
        ...current,
        aircraftCount: (updatedScenario?.aircraft || []).length || current.aircraftCount,
      }));
      setDuplicateScenarioName(defaultDuplicateScenarioName(updatedScenario?.name));
      setStatus({ kind: 'success', message: 'Scenario saved.' });
    } catch (error) {
      if (isAbortError(error)) {
        return;
      }
      setStatus({ kind: 'error', message: error.message });
    } finally {
      setScenarioBusy(false);
    }
  }

  async function handleNextRound() {
    if (nextRoundInFlightRef.current) {
      return;
    }
    if (!gameId) {
      setStatus({ kind: 'error', message: 'Create a game first.' });
      return;
    }
    if (!isValidGameId(gameId)) {
      setStatus({ kind: 'error', message: 'Game ID is invalid. Create a new game first.' });
      return;
    }
    nextRoundInFlightRef.current = true;
    setNextRoundCountdown(null);
    setStatus({ kind: 'loading', message: 'Starting next round and assigning missions...' });
    try {
      // Automated mode keeps the previous one-click flow. Manual mode now splits this
      // into planning first and mission resolution as an explicit second step.
      const data = await request(
        automationEnabled ? `/games/${gameId}/rounds/next` : `/games/${gameId}/rounds/plan`,
        { method: 'POST' }
      );
      setLastAutoResponse(data);
      if (!automationEnabled) {
        setManualMissionPreviewAssignments({});
        applyGameState(data.gameState);
        setStatus({
          kind: finalGameStatusKind(data.gameState, 'success'),
          message: data.gameFinished
            ? finalGameMessage(data.gameState)
            : `Round planned. Next action: ${humanizeAction(data.nextAction)}.`,
        });
      } else {
        const previewSeconds = Math.max(0, Number(diceAutomation.missionPreviewSeconds || 0));
        const previewState = buildMissionPreviewState(gameState, data.autoAssignments);
        if (previewState && previewSeconds > 0) {
        clearMissionPreview();
        gameStateRef.current = previewState;
        setPreviousGameState(gameState);
        setGameState(previewState);
        setMissionPreviewActive(true);
        setStatus({
          kind: 'idle',
          message: `Showing mission preview for ${previewSeconds}s before mission resolution.`,
        });
        missionPreviewTimerRef.current = window.setTimeout(() => {
          setPreviousGameState(previewState);
          gameStateRef.current = data.gameState;
          setGameState(data.gameState);
          setMissionPreviewActive(false);
          missionPreviewTimerRef.current = null;
          setStatus({
            kind: finalGameStatusKind(data.gameState, 'success'),
            message: data.gameFinished
              ? finalGameMessage(data.gameState)
              : `Round prepared. Next action: ${humanizeAction(data.nextAction)}.`,
          });
        }, previewSeconds * 1000);
        } else {
          applyGameState(data.gameState);
          setStatus({
            kind: finalGameStatusKind(data.gameState, 'success'),
            message: data.gameFinished
              ? finalGameMessage(data.gameState)
              : `Round prepared. Next action: ${humanizeAction(data.nextAction)}.`,
          });
        }
      }
      pushLog(data.gameFinished ? finalGameLogTitle(data.gameState) : (automationEnabled ? 'Autoplay round start' : 'Manual round plan'), enrichGameFinishedPayload(data));
    } catch (error) {
      if (isAbortError(error)) {
        return;
      }
      setStatus({ kind: 'error', message: error.message });
    } finally {
      nextRoundInFlightRef.current = false;
    }
  }

  async function handleResolveMissions() {
    if (!isValidGameId(gameId)) {
      setStatus({ kind: 'error', message: 'Game ID is invalid. Create a new game first.' });
      return;
    }
    // This step exists only to make the manual flow observable: planned aircraft stay in
    // "On mission" until the operator explicitly resolves missions into dice handling.
    setStatus({ kind: 'loading', message: 'Resolving planned missions...' });
    try {
      const data = await request(`/games/${gameId}/missions/resolve-auto`, { method: 'POST' });
      setLastAutoResponse(data);
      setManualMissionPreviewAssignments({});
      applyGameState(data.gameState);
      setStatus({
        kind: finalGameStatusKind(data.gameState, 'success'),
        message: data.gameFinished
          ? finalGameMessage(data.gameState)
          : `Mission resolution completed. Next action: ${humanizeAction(data.nextAction)}.`,
      });
      pushLog(data.gameFinished ? finalGameLogTitle(data.gameState) : 'Manual mission resolution', enrichGameFinishedPayload(data));
    } catch (error) {
      if (isAbortError(error)) {
        return;
      }
      setStatus({ kind: 'error', message: error.message });
    }
  }

  async function handleRollDice(event) {
    event.preventDefault();
    if (!gameId || !selectedAircraft) {
      setStatus({ kind: 'error', message: 'Select an aircraft waiting for a dice roll.' });
      return;
    }
    if (!isValidGameId(gameId)) {
      setStatus({ kind: 'error', message: 'Game ID is invalid. Create a new game first.' });
      return;
    }
    await submitDiceRoll(
      selectedAircraft,
      useRandomDice ? randomDiceValue() : Number(diceValue),
      false,
      manualDiceSelectionMode(useRandomDice)
    );
  }

  async function submitDiceRoll(aircraftCode, resolvedDiceValue, automated, diceSelectionMode) {
    const latestState = gameStateRef.current;
    const stillPending = (latestState?.aircraft || []).some(
      (aircraft) => aircraft.code === aircraftCode && aircraft.status === 'AWAITING_DICE_ROLL'
    );
    if (latestState?.game?.roundPhase !== 'DICE_ROLL' || !stillPending) {
      if (isValidGameId(gameId)) {
        try {
          await refreshGameState(gameId);
        } catch (error) {
          if (isAbortError(error)) {
            return;
          }
          setStatus({ kind: 'error', message: error.message });
          return;
        }
      }
      setStatus({
        kind: 'idle',
        message: 'Dice step already completed. Game state refreshed.',
      });
      return;
    }

    setStatus({
      kind: 'loading',
      message: automated
        ? `Automating dice roll for ${aircraftCode} in round ${gameState?.game?.currentRound ?? '-' }...`
        : `Submitting dice roll for ${aircraftCode}...`,
    });
    try {
      const data = await request(`/games/${gameId}/dice-rolls/auto`, {
        method: 'POST',
        body: JSON.stringify({
          aircraftCode,
          diceValue: resolvedDiceValue,
          diceSelectionMode,
        }),
      });
      setManualMissionPreviewAssignments((current) => {
        if (!current[aircraftCode]) {
          return current;
        }
        const nextAssignments = { ...current };
        delete nextAssignments[aircraftCode];
        return nextAssignments;
      });
      setLastAutoResponse(data);
      applyGameState(data.gameState);
      setStatus({
        kind: data.gameFinished ? finalGameStatusKind(data.gameState, 'success') : 'idle',
        message: data.gameFinished
          ? finalGameMessage(data.gameState)
          : automated
            ? `Automated dice resolved. Next action: ${humanizeAction(data.nextAction)}.`
            : `Dice resolved. Next action: ${humanizeAction(data.nextAction)}.`,
      });
      pushLog(data.gameFinished ? finalGameLogTitle(data.gameState) : (automated ? `Automated dice for ${aircraftCode}` : `Dice submitted for ${aircraftCode}`), enrichGameFinishedPayload({
        ...data,
        diceRoll: {
          aircraftCode,
          diceValue: resolvedDiceValue,
        },
      }));
    } catch (error) {
      if (isAbortError(error)) {
        return;
      }
      if (String(error.message).includes('Round is in phase LANDING')) {
        try {
          await refreshGameState(gameId);
          setStatus({
            kind: 'idle',
            message: 'Dice step had already finished. Game state refreshed.',
          });
          return;
        } catch (refreshError) {
          if (isAbortError(refreshError)) {
            return;
          }
          setStatus({ kind: 'error', message: refreshError.message });
          return;
        }
      }
      setStatus({ kind: 'error', message: error.message });
    }
  }

  return (
    <main className="shell">
      <section className="topbar">
        <div className="brand-card brand-card-wide">Smart Air Base</div>
        <div className="headline-divider" />
        <div className="top-mode-tabs">
          <div className="mode-tabs" role="tablist" aria-label="Workspace mode">
            <button
              type="button"
              role="tab"
              aria-selected={currentView === 'PLAY'}
              className={`mode-tab ${currentView === 'PLAY' ? 'mode-tab-active' : ''}`}
              disabled={simulationStatus.running}
              onClick={() => setCurrentView('PLAY')}
            >
              Play
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={currentView === 'SIMULATOR'}
              className={`mode-tab ${currentView === 'SIMULATOR' ? 'mode-tab-active' : ''}`}
              disabled={hasOngoingGame}
              onClick={() => setCurrentView('SIMULATOR')}
            >
              Simulator
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={currentView === 'SCENARIOS'}
              className={`mode-tab ${currentView === 'SCENARIOS' ? 'mode-tab-active' : ''}`}
              // Keep live play isolated from scenario editing until the game is
              // finished or aborted, so the control panel stays single-purpose.
              disabled={workspaceLocked}
              onClick={() => setCurrentView('SCENARIOS')}
            >
              Scenario editor
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={currentView === 'DASHBOARD'}
              className={`mode-tab ${currentView === 'DASHBOARD' ? 'mode-tab-active' : ''}`}
              onClick={() => setCurrentView('DASHBOARD')}
            >
              Dashboard
            </button>
          </div>
          <p className="workspace-intro">
            {currentView === 'PLAY'
              ? 'Play a single game, progress rounds, choose dice handling, and follow the live result.'
              : currentView === 'SIMULATOR'
                ? 'Run multiple games without visual playback to compare outcomes for a chosen setup and dice strategy.'
                : currentView === 'SCENARIOS'
                  ? 'Duplicate and tune your own scenario settings while keeping the standard template locked.'
                  : 'Inspect finished-game analytics rows and filter them by setup so recent runs are easy to compare.'}
          </p>
        </div>
        {currentView === 'PLAY' ? (
          <div className="status-stack">
            <MetricCard label="Game status" value={humanizeStatus(gameState?.game?.status || 'Not started')} />
            <MetricCard
              label="Scenario setup"
              value={`${gameState?.aircraft?.length || createForm.aircraftCount || rules?.initialSetup?.aircraftCount || 0} planes / ${gameState?.missions?.length || totalConfiguredMissionCount(createForm.missionTypeCounts)} missions`}
            />
            <MetricCard label="Round" value={gameState?.game?.currentRound ?? 0} />
            <MetricCard label="Mission status" value={`${completedMissionCount(gameState)}/${gameState?.missions?.length || 0} Complete`} />
            <MetricCard label="Holding status" value={`${holdingCount(gameState)} planes`} />
            <MetricCard label="Crash status" value={`${crashedCount(gameState)} destroyed`} />
          </div>
        ) : null}
      </section>

      {currentView === 'SIMULATOR' ? (
        <section className="scenario-browser">
          <article className="info-panel scenario-detail-panel">
            <h3>Simulator</h3>
            <form className="scenario-editor-sections" onSubmit={handleStartSimulation}>
              <label>
                Batch name
                <input
                  value={simulationForm.batchName}
                  onChange={(event) => setSimulationForm((current) => ({
                    ...current,
                    batchName: normalizeScenarioTemplateName(event.target.value),
                  }))}
                  disabled={workspaceLocked}
                />
              </label>
              <label>
                Scenario
                <select
                  value={selectedScenarioId}
                  onChange={(event) => setSelectedScenarioId(event.target.value)}
                  disabled={workspaceLocked}
                >
                  {scenarios.map((scenario) => (
                    <option key={scenario.scenarioId} value={scenario.scenarioId}>
                      {scenario.name}
                    </option>
                  ))}
                </select>
              </label>
              <div className="scenario-config-grid">
                <label>
                  Runs
                  <input
                    type="number"
                    min="1"
                    value={simulationForm.runCount}
                    onChange={(event) => setSimulationForm((current) => ({
                      ...current,
                      runCount: Math.max(1, Number(event.target.value) || 1),
                    }))}
                    disabled={workspaceLocked}
                  />
                </label>
                <label>
                  Max rounds
                  <input
                    type="number"
                    min="1"
                    value={simulationForm.maxRounds}
                    onChange={(event) => setSimulationForm((current) => ({
                      ...current,
                      maxRounds: Math.max(1, Number(event.target.value) || 1000),
                    }))}
                    disabled={workspaceLocked}
                  />
                  <span className="field-warning-copy">Upper limit for how many rounds a single run may take.</span>
                </label>
                <label>
                  Aircraft
                  <input
                    type="number"
                    min="1"
                    max={totalScenarioParkingCapacity || 8}
                    value={simulationForm.aircraftCount}
                    onChange={(event) => setSimulationForm((current) => ({
                      ...current,
                      aircraftCount: Math.max(1, Math.min(totalScenarioParkingCapacity || 8, Number(event.target.value) || 1)),
                    }))}
                    disabled={workspaceLocked}
                  />
                </label>
                <label>
                  M1 missions
                  <input
                    type="number"
                    min="0"
                    value={simulationForm.missionTypeCounts.M1}
                    onChange={(event) => setSimulationForm((current) => ({
                      ...current,
                      missionTypeCounts: { ...current.missionTypeCounts, M1: Math.max(0, Number(event.target.value) || 0) },
                    }))}
                    disabled={workspaceLocked}
                  />
                </label>
                <label>
                  M2 missions
                  <input
                    type="number"
                    min="0"
                    value={simulationForm.missionTypeCounts.M2}
                    onChange={(event) => setSimulationForm((current) => ({
                      ...current,
                      missionTypeCounts: { ...current.missionTypeCounts, M2: Math.max(0, Number(event.target.value) || 0) },
                    }))}
                    disabled={workspaceLocked}
                  />
                </label>
                <label>
                  M3 missions
                  <input
                    type="number"
                    min="0"
                    value={simulationForm.missionTypeCounts.M3}
                    onChange={(event) => setSimulationForm((current) => ({
                      ...current,
                      missionTypeCounts: { ...current.missionTypeCounts, M3: Math.max(0, Number(event.target.value) || 0) },
                    }))}
                    disabled={workspaceLocked}
                  />
                </label>
                <label>
                  Dice strategy
                  <select
                    value={simulationForm.diceStrategy}
                    onChange={(event) => setSimulationForm((current) => ({ ...current, diceStrategy: event.target.value }))}
                    disabled={workspaceLocked}
                  >
                    <option value="RANDOM">Random dice outcome</option>
                    <option value="MIN_DAMAGE">Favor as little damage as possible</option>
                    <option value="MAX_DAMAGE">Cause as much damage as possible</option>
                  </select>
                </label>
              </div>
              <div className="button-row simulation-action-row">
                <button
                  type="submit"
                  className="active-button compact-button simulation-action-button"
                  disabled={workspaceLocked || !simulationForm.batchName.trim()}
                >
                  Start simulation
                </button>
              </div>
              <p className={simulationStatus.message?.toLowerCase().includes('error') || simulationStatus.message?.toLowerCase().includes('failed') ? 'field-warning-copy' : 'muted-copy'}>
                {simulationStatus.message}
              </p>
              {simulationStatus.requestedRuns && (simulationStatus.running || simulationStatus.completedRuns > 0 || simulationStatus.failedRuns > 0) ? (
                <p className="muted-copy">
                  {simulationStatus.running ? <span className="simulation-running-indicator" aria-hidden="true" /> : null}
                  Completed {simulationStatus.completedRuns}/{simulationStatus.requestedRuns}
                  {simulationStatus.failedRuns ? ` · Failed ${simulationStatus.failedRuns}` : ''}
                  {simulationStatus.currentGameName ? ` · Current ${simulationStatus.currentGameName}` : ''}
                </p>
              ) : null}
            </form>

            {(simulationStatus.running || simulationStatus.completedRuns > 0 || simulationStatus.failedRuns > 0) ? (
              <section className="scenario-reference-section simulation-results-panel">
                <div className="section-heading">
                  <h4>Simulation results</h4>
                  <p className="muted-copy">Aggregated outcomes for the current batch. Starting a new simulation resets this panel.</p>
                </div>
                <div className="simulation-metrics-grid">
                  <MetricCard label="Batch" value={simulationStatus.batchName || 'N/A'} />
                  <MetricCard label="Scenario" value={simulationStatus.scenarioName || 'N/A'} />
                  <MetricCard label="Elapsed time" value={formatElapsedTime(simulationElapsedSeconds)} />
                  <MetricCard label="Progress" value={`${simulationStatus.completedRuns}/${simulationStatus.requestedRuns || 0}`} />
                  <MetricCard label="Won" value={simulationStatus.wonRuns} />
                  <MetricCard label="Lost" value={simulationStatus.lostRuns} />
                  <MetricCard label="Failed" value={simulationStatus.failedRuns} />
                  <MetricCard label="Current run" value={simulationStatus.currentGameName || (simulationStatus.running ? 'Starting...' : 'Complete')} />
                  <MetricCard label="Aircraft" value={simulationStatus.aircraftCount} />
                  <MetricCard label="Max rounds" value={simulationStatus.maxRounds} />
                  <MetricCard label="Dice strategy" value={humanizeStatus(simulationStatus.diceStrategy || 'RANDOM')} />
                  <MetricCard
                    label="Mission mix"
                    value={(
                      <div className="metric-card-stack">
                        <div>M1: {simulationStatus.missionTypeCounts.M1 || 0}</div>
                        <div>M2: {simulationStatus.missionTypeCounts.M2 || 0}</div>
                        <div>M3: {simulationStatus.missionTypeCounts.M3 || 0}</div>
                      </div>
                    )}
                  />
                </div>
              </section>
            ) : null}
          </article>

          <article className="info-panel scenario-detail-panel">
            <h3>Scenario overview</h3>
            {selectedScenario ? (
              <>
                <section className="scenario-reference-section">
                  <div className="section-heading">
                    <h4>{selectedScenario.name}</h4>
                    <p className="muted-copy">
                      {Number(simulationForm.aircraftCount || 0)} aircraft · {totalConfiguredMissionCount(simulationForm.missionTypeCounts)} missions · {(selectedScenario.bases || []).length} bases
                    </p>
                  </div>
                  {selectedScenario.description ? <p>{selectedScenario.description}</p> : null}
                  <div>
                    <p className="muted-copy">Simulation setup</p>
                    <ul className="compact-list">
                      {simulationMissionSummary.map((mission) => (
                        <li key={mission.code}>{mission.name}: {mission.count}</li>
                      ))}
                    </ul>
                  </div>
                </section>

                <section className="scenario-reference-section">
                  <div className="section-heading">
                    <h4>Base overview</h4>
                    <p className="muted-copy">Configured capacities and starting resources for the selected scenario.</p>
                  </div>
                  <ul className="compact-list">
                    {(selectedScenario.bases || []).map((base) => (
                      <li key={base.code}>
                        {base.code} · {BASE_TYPE_LABELS[base.baseTypeCode] || base.name || base.baseTypeCode || 'Base'} ·
                        {' '}Parking {base.parkingCapacity ?? 0} ·
                        {' '}Repair {isFuelOutpostScenarioBase(base) ? 0 : (base.maintenanceCapacity ?? 0)} ·
                        {' '}Fuel {base.fuelStart ?? 0}/{base.fuelMax ?? 0} ·
                        {' '}Weapons {base.weaponsStart ?? 0}/{base.weaponsMax ?? 0} ·
                        {' '}Spare parts {base.sparePartsStart ?? 0}/{base.sparePartsMax ?? 0}
                      </li>
                    ))}
                  </ul>
                </section>

                <section className="scenario-reference-section">
                  <div className="section-heading">
                    <h4>Deliveries</h4>
                    <p className="muted-copy">Delivery amounts stay tied to each base and keep their configured frequency.</p>
                  </div>
                  <ul className="compact-list">
                    {(selectedScenario.bases || []).flatMap((base) => (
                      (base.supplyRules || []).map((rule) => (
                        <li key={`${base.code}-${rule.resource}`}>
                          {base.code} · {humanizeStatus(rule.resource)} {rule.deliveryAmount > 0 ? `+${rule.deliveryAmount}` : rule.deliveryAmount} · Every {rule.frequencyRounds} rounds
                        </li>
                      ))
                    ))}
                  </ul>
                </section>

                <section className="scenario-reference-section">
                  <div className="section-heading">
                    <h4>{selectedScenarioRules.title}</h4>
                    <p className="muted-copy">{selectedScenarioRules.summary}</p>
                  </div>
                  <ul className="compact-list scenario-rules-list">
                    {selectedScenarioRules.points.map((point) => (
                      <li key={point}>{point}</li>
                    ))}
                  </ul>
                </section>
              </>
            ) : (
              <p className="muted-copy">Select a scenario to inspect it.</p>
            )}
          </article>
        </section>
      ) : null}

      {currentView === 'SCENARIOS' ? (
        <section className="scenario-browser">
          <article className="event-panel">
            <h3>Scenario library</h3>
            <div className="scenario-list">
              {scenarios.map((scenario) => (
                <button
                  key={scenario.scenarioId}
                  type="button"
                  className={`scenario-list-item ${String(scenario.scenarioId) === String(selectedScenarioId) ? 'scenario-list-item-active' : ''}`}
                  onClick={() => setSelectedScenarioId(String(scenario.scenarioId))}
                >
                  <strong>{scenario.name}</strong>
                  <span>{humanizeStatus(scenario.sourceType)}</span>
                </button>
              ))}
            </div>
          </article>
          <article className="info-panel scenario-detail-panel">
            <h3>Scenario details</h3>
            {selectedScenario ? (
              <>
                <p><strong>{selectedScenario.name}</strong></p>
                <p>Type: {humanizeStatus(selectedScenario.sourceType)}. Editable: {selectedScenario.editable ? 'Yes' : 'No'}. Deletable: {selectedScenario.deletable ? 'Yes' : 'No'}.</p>
                <section className="scenario-reference-section">
                  <header className="section-heading">
                    <h4>Scenario overview</h4>
                    <p className="muted-copy">Reference information for the selected scenario, including deliveries and the fixed repair rules.</p>
                  </header>
                  <div className="scenario-summary-grid">
                    <div>
                      <h4>Bases</h4>
                      <ul className="compact-list">
                        {(selectedScenario.bases || []).map((base) => (
                          <li key={base.code}>{base.code} {BASE_TYPE_LABELS[base.baseTypeCode] || base.name} · park {base.parkingCapacity} · repair {base.maintenanceCapacity}</li>
                        ))}
                      </ul>
                    </div>
                    <div>
                      <h4>Game rules</h4>
                      <ul className="compact-list">
                        {(selectedScenario.diceRules || []).map((rule) => (
                          <li key={rule.diceValue}>
                            {rule.diceValue} = {humanizeStatus(rule.damageType)} · spare parts {rule.sparePartsCost ?? 0} · {rule.requiresFullService ? 'Full service' : `${rule.repairRounds} rounds`}
                          </li>
                        ))}
                      </ul>
                    </div>
                    <div className="scenario-summary-wide">
                      <h4>Deliveries</h4>
                      <ul className="compact-list">
                        {(selectedScenario.bases || []).map((base) => (
                          <li key={`${base.code}-deliveries`}>
                            {base.code} {BASE_TYPE_LABELS[base.baseTypeCode] || base.name}: {' '}
                            {(base.supplyRules || []).length
                              ? base.supplyRules.map((rule) => (
                                `${humanizeStatus(rule.resource)} +${rule.deliveryAmount} every ${rule.frequencyRounds} rounds`
                              )).join(' · ')
                              : 'No scheduled deliveries'}
                          </li>
                        ))}
                      </ul>
                    </div>
                    <div className="scenario-summary-wide">
                      <h4>Base resources</h4>
                      <ul className="compact-list">
                        {(selectedScenario.bases || []).map((base) => (
                          <li key={`${base.code}-resources`}>
                            {base.code} {BASE_TYPE_LABELS[base.baseTypeCode] || base.name}: {' '}
                            Fuel {base.fuelStart ?? 0}/{base.fuelMax ?? 0} ·
                            {' '}Weapons {base.weaponsStart ?? 0}/{base.weaponsMax ?? 0} ·
                            {' '}Spare parts {base.sparePartsStart ?? 0}/{base.sparePartsMax ?? 0}
                          </li>
                        ))}
                      </ul>
                      <p className="muted-copy">Values are shown as start/max per base.</p>
                    </div>
                  </div>
                </section>
                <label>
                  Duplicate as
                  <input
                    value={duplicateScenarioName}
                    onChange={(event) => setDuplicateScenarioName(normalizeScenarioTemplateName(event.target.value))}
                    aria-invalid={!hasValidDuplicateScenarioName}
                    disabled={scenarioBusy}
                  />
                </label>
                {!hasValidDuplicateScenarioName ? (
                  <p className="muted-copy">Name is required and must contain at least 1 uppercase letter, digit, or underscore.</p>
                ) : null}
                <div className="button-row">
                  <button
                    type="button"
                    className={selectedScenario.sourceType === 'SYSTEM' ? 'compact-button' : ''}
                    onClick={handleDuplicateScenario}
                    disabled={scenarioBusy || !hasValidDuplicateScenarioName}
                  >
                    Duplicate
                  </button>
                  {selectedScenario.editable ? (
                    <button type="button" onClick={handleSaveScenario} disabled={scenarioBusy}>Save scenario</button>
                  ) : null}
                  {selectedScenario.deletable ? (
                    <button type="button" className="ghost-button" onClick={handleDeleteScenario} disabled={scenarioBusy}>Delete scenario</button>
                  ) : null}
                </div>
                {status.message && ['success', 'loading', 'error'].includes(status.kind) && status.message.includes('Scenario') ? (
                  <p className={`status-pill status-${status.kind}`}>{status.message}</p>
                ) : null}
                <section className="scenario-editor-sections">
                  <header className="section-heading">
                    <h4>Editable settings</h4>
                    <p className="muted-copy">Change only the scenario data that is allowed to vary. System scenarios stay read-only.</p>
                  </header>

                  <article className="scenario-editor-card">
                    <header className="section-heading">
                      <h4>Scenario description</h4>
                      <p className="muted-copy">Update the descriptive text for this scenario when the scenario is editable.</p>
                    </header>
                    <label>
                      Description
                      <textarea
                        rows="3"
                        value={selectedScenario.description || ''}
                        disabled={scenarioBusy || !selectedScenario.editable}
                        onChange={(event) => setSelectedScenario((current) => (
                          current ? { ...current, description: event.target.value } : current
                        ))}
                      />
                    </label>
                  </article>

                  <article className="scenario-editor-card">
                    <header className="section-heading">
                      <h4>Aircraft settings</h4>
                      <p className="muted-copy">Review the aircraft types already used in this scenario. You can adjust existing aircraft instances, but not add new aircraft types.</p>
                    </header>
                    <div className="scenario-config-grid scenario-config-grid-aircraft">
                      <div className="scenario-config-row scenario-config-head">
                        <span>Aircraft type</span>
                        <span>Initial aircraft count</span>
                        <span>Fuel</span>
                        <span>Weapons</span>
                        <span>Flight hours</span>
                      </div>
                      {scenarioAircraftGroups.map((group) => (
                        <div key={group.typeCode} className="scenario-config-row">
                          <span className="scenario-row-label">{group.typeCode}</span>
                          <input
                            type="number"
                            min="1"
                            max={Math.max(1, totalScenarioParkingCapacity - ((selectedScenario.aircraft || []).length - group.aircraft.length))}
                            value={group.aircraft.length}
                            disabled={scenarioBusy || !selectedScenario.editable}
                            onChange={(event) => updateScenarioAircraftGroupCount(group.typeCode, event.target.value)}
                          />
                          <input
                            type="number"
                            min="0"
                            value={group.representative.fuelStart ?? 0}
                            disabled={scenarioBusy || !selectedScenario.editable}
                            onChange={(event) => updateScenarioAircraftGroupNumberField(group.typeCode, 'fuelStart', event.target.value)}
                          />
                          <input
                            type="number"
                            min="0"
                            value={group.representative.weaponsStart ?? 0}
                            disabled={scenarioBusy || !selectedScenario.editable}
                            onChange={(event) => updateScenarioAircraftGroupNumberField(group.typeCode, 'weaponsStart', event.target.value)}
                          />
                          <input
                            type="number"
                            min="0"
                            value={group.representative.flightHoursStart ?? 0}
                            disabled={scenarioBusy || !selectedScenario.editable}
                            onChange={(event) => updateScenarioAircraftGroupNumberField(group.typeCode, 'flightHoursStart', event.target.value)}
                          />
                        </div>
                      ))}
                    </div>
                  </article>

                  <article className="scenario-editor-card">
                    <header className="section-heading">
                      <h4>Mission settings</h4>
                      <p className="muted-copy">Mission types are fixed. You can only adjust what each mission consumes.</p>
                    </header>
                    <div className="scenario-config-grid scenario-config-grid-missions">
                      <div className="scenario-config-row scenario-config-head">
                        <span>Mission</span>
                        <span>Type</span>
                        <span>Fuel cost</span>
                        <span>Weapon cost</span>
                        <span>Flight time</span>
                      </div>
                      {(selectedScenario.missions || []).map((mission) => (
                        <div key={mission.code} className="scenario-config-row">
                          <span className="scenario-row-label">{mission.code}</span>
                          <span>{mission.missionTypeName || mission.missionTypeCode}</span>
                          <input
                            type="number"
                            min="0"
                            value={mission.fuelCost ?? 0}
                            disabled={scenarioBusy || !selectedScenario.editable}
                            onChange={(event) => updateScenarioNumberField('missions', mission.code, 'fuelCost', event.target.value)}
                          />
                          <input
                            type="number"
                            min="0"
                            value={mission.weaponCost ?? 0}
                            disabled={scenarioBusy || !selectedScenario.editable}
                            onChange={(event) => updateScenarioNumberField('missions', mission.code, 'weaponCost', event.target.value)}
                          />
                          <input
                            type="number"
                            min="0"
                            value={mission.flightTimeCost ?? 0}
                            disabled={scenarioBusy || !selectedScenario.editable}
                            onChange={(event) => updateScenarioNumberField('missions', mission.code, 'flightTimeCost', event.target.value)}
                          />
                        </div>
                      ))}
                    </div>
                  </article>

                  <article className="scenario-editor-card">
                    <header className="section-heading">
                      <h4>Base settings</h4>
                      <p className="muted-copy">Bases are fixed. You can change existing slot capacities, base start/max resources, and delivery amounts for the bases already in this scenario.</p>
                    </header>
                    <div className="scenario-subsection">
                      <h5 className="scenario-subsection-title">Capacity</h5>
                      <div className="scenario-config-grid scenario-config-grid-bases">
                        <div className="scenario-config-row scenario-config-head scenario-base-capacity-row">
                          <span>Base</span>
                          <span>Name</span>
                          <span>Parking slots</span>
                          <span>Repair slots</span>
                        </div>
                        {(selectedScenario.bases || []).map((base, index) => {
                          const supportsRepairSlots = !isFuelOutpostScenarioBase(base);
                          const isFuelOutpostBase = !supportsRepairSlots;
                          return (
                            <div key={`${base.code}-capacity-group`} className="scenario-base-rule-group">
                              <div key={`${base.code}-capacity`} className={`scenario-config-row scenario-base-capacity-row ${index > 0 ? 'scenario-base-separator' : ''}`}>
                                <span className="scenario-row-label">{base.code}</span>
                                <span>{BASE_TYPE_LABELS[base.baseTypeCode] || base.name || base.baseTypeCode || 'Base'}</span>
                                <input
                                  type="number"
                                  min="0"
                                  value={base.parkingCapacity ?? 0}
                                  disabled={scenarioBusy || !selectedScenario.editable}
                                  onChange={(event) => updateScenarioNumberField('bases', base.code, 'parkingCapacity', event.target.value)}
                                />
                                {supportsRepairSlots ? (
                                  <input
                                    type="number"
                                    min="0"
                                    value={base.maintenanceCapacity ?? 0}
                                    disabled={scenarioBusy || !selectedScenario.editable}
                                    onChange={(event) => updateScenarioNumberField('bases', base.code, 'maintenanceCapacity', event.target.value)}
                                  />
                                ) : (
                                  <input
                                    className="scenario-constant-zero"
                                    type="number"
                                    min="0"
                                    value={0}
                                    disabled
                                    readOnly
                                  />
                                )}
                              </div>
                              {isFuelOutpostBase ? (
                                <div key={`${base.code}-capacity-note`} className="scenario-base-rule-note scenario-base-rule-note-critical">
                                  Repair slots are fixed at 0 for Base C because this base is refuel-only.
                                </div>
                              ) : null}
                            </div>
                          );
                        })}
                      </div>
                    </div>

                    <div className="scenario-subsection">
                      <h5 className="scenario-subsection-title">Resources</h5>
                      <div className="scenario-config-grid scenario-config-grid-base-resources">
                        <div className="scenario-config-row scenario-config-head scenario-base-resource-row">
                          <span>Base</span>
                          <span>Resource</span>
                          <span>Start</span>
                          <span>Max</span>
                        </div>
                        {(selectedScenario.bases || []).flatMap((base, baseIndex) => {
                          const isFuelOutpostBase = isFuelOutpostScenarioBase(base);
                          const resourceRows = [
                            ['Fuel', 'fuelStart', 'fuelMax', true],
                            ['Weapons', 'weaponsStart', 'weaponsMax', !isFuelOutpostBase],
                            ['Spare parts', 'sparePartsStart', 'sparePartsMax', !isFuelOutpostBase],
                          ].map(([label, startField, maxField, editable], index) => (
                            <div
                              key={`${base.code}-${label}`}
                              className={`scenario-config-row scenario-base-resource-row ${baseIndex > 0 && index === 0 ? 'scenario-base-separator' : ''}`}
                            >
                              <span className="scenario-row-label">{index === 0 ? base.code : ''}</span>
                              <span>{label}</span>
                              {editable ? (
                                <>
                                  <input
                                    type="number"
                                    min="0"
                                    value={base[startField] ?? 0}
                                    disabled={scenarioBusy || !selectedScenario.editable}
                                    onChange={(event) => updateScenarioNumberField('bases', base.code, startField, event.target.value)}
                                  />
                                  <input
                                    type="number"
                                    min="0"
                                    value={base[maxField] ?? 0}
                                    disabled={scenarioBusy || !selectedScenario.editable}
                                    onChange={(event) => updateScenarioNumberField('bases', base.code, maxField, event.target.value)}
                                  />
                                </>
                              ) : (
                                <>
                                  <input className="scenario-constant-zero" type="number" min="0" value={0} disabled readOnly />
                                  <input className="scenario-constant-zero" type="number" min="0" value={0} disabled readOnly />
                                </>
                              )}
                            </div>
                          ));

                          if (isFuelOutpostBase) {
                            resourceRows.push(
                              <div key={`${base.code}-resource-note`} className="scenario-base-rule-note scenario-base-rule-note-critical">
                                Weapons and spare-parts stocks are fixed at 0 for Base C because this base type is refuel-only.
                              </div>
                            );
                          }

                          return resourceRows;
                        })}
                      </div>
                    </div>

                    <div className="scenario-subsection">
                      <h5 className="scenario-subsection-title">Deliveries</h5>
                      <div className="scenario-config-grid scenario-config-grid-base-deliveries">
                        <div className="scenario-config-row scenario-config-head scenario-base-delivery-row">
                          <span>Base</span>
                          <span>Delivery</span>
                          <span>Amount</span>
                          <span>Frequency</span>
                        </div>
                        {(selectedScenario.bases || []).flatMap((base, baseIndex) => {
                          const isFuelOutpostBase = isFuelOutpostScenarioBase(base);
                          const deliveryRows = (base.supplyRules || []).map((rule, index) => (
                            <div
                              key={`${base.code}-${rule.resource}`}
                              className={`scenario-config-row scenario-base-delivery-row ${baseIndex > 0 && index === 0 ? 'scenario-base-separator' : ''}`}
                            >
                              <span className="scenario-row-label">{index === 0 ? base.code : ''}</span>
                              <span>{humanizeStatus(rule.resource)}</span>
                              {isFuelOutpostBase && rule.resource !== 'FUEL' ? (
                                <input className="scenario-constant-zero" type="number" min="0" value={0} disabled readOnly />
                              ) : (
                                <input
                                  type="number"
                                  min="0"
                                  value={rule.deliveryAmount ?? 0}
                                  disabled={scenarioBusy || !selectedScenario.editable}
                                  onChange={(event) => updateScenarioSupplyRuleNumberField(base.code, rule.resource, event.target.value)}
                                />
                              )}
                              <span className="scenario-inline-note">Every {rule.frequencyRounds} rounds</span>
                            </div>
                          ));

                          if (isFuelOutpostBase && !(base.supplyRules || []).some((rule) => rule.resource === 'WEAPONS')) {
                            deliveryRows.push(
                              <div key={`${base.code}-WEAPONS-constant`} className="scenario-config-row scenario-base-delivery-row scenario-base-rule-row">
                                <span className="scenario-row-label" />
                                <span>Weapons</span>
                                <input className="scenario-constant-zero" type="number" min="0" value={0} disabled readOnly />
                                <span className="scenario-base-rule-note scenario-base-rule-note-critical">Not available for Base C</span>
                              </div>
                            );
                          }
                          if (isFuelOutpostBase && !(base.supplyRules || []).some((rule) => rule.resource === 'SPARE_PARTS')) {
                            deliveryRows.push(
                              <div key={`${base.code}-SPARE_PARTS-constant`} className="scenario-config-row scenario-base-delivery-row scenario-base-rule-row">
                                <span className="scenario-row-label" />
                                <span>Spare parts</span>
                                <input className="scenario-constant-zero" type="number" min="0" value={0} disabled readOnly />
                                <span className="scenario-base-rule-note scenario-base-rule-note-critical">Not available for Base C</span>
                              </div>
                            );
                          }
                          if (isFuelOutpostBase) {
                            deliveryRows.push(
                              <div key={`${base.code}-delivery-note`} className="scenario-base-rule-note scenario-base-rule-note-critical">
                                Weapons and spare-parts deliveries do not apply to Base C in any scenario. This base remains fuel-only.
                              </div>
                            );
                          }

                          return deliveryRows;
                        })}
                      </div>
                    </div>
                  </article>
                </section>
              </>
            ) : (
              <p className="muted-copy">Select a scenario to inspect it.</p>
            )}
          </article>
        </section>
      ) : currentView === 'DASHBOARD' ? (
        <section className="dashboard-layout">
          <article className="info-panel dashboard-panel">
            <h3>Dashboard</h3>
            <div className="dashboard-filters">
              <label>
                Scenario
                <select
                  value={dashboardFilters.scenarioName}
                  onChange={(event) => setDashboardFilters((current) => ({ ...current, scenarioName: event.target.value }))}
                >
                  <option value="">All</option>
                  {dashboardFilterOptions.scenarios.map((scenarioName) => (
                    <option key={`dashboard-scenario-${scenarioName}`} value={scenarioName}>{scenarioName}</option>
                  ))}
                </select>
              </label>
              <label>
                Run date
                <select
                  value={dashboardFilters.createdDate}
                  onChange={(event) => setDashboardFilters((current) => ({ ...current, createdDate: event.target.value }))}
                >
                  <option value="">All</option>
                  {dashboardFilterOptions.dates.map((createdDate) => (
                    <option key={`dashboard-date-${createdDate}`} value={createdDate}>{createdDate}</option>
                  ))}
                </select>
              </label>
              <label>
                Aircraft
                <select
                  value={dashboardFilters.aircraftCount}
                  onChange={(event) => setDashboardFilters((current) => ({ ...current, aircraftCount: event.target.value }))}
                >
                  <option value="">All</option>
                  {dashboardFilterOptions.aircraftCounts.map((value) => (
                    <option key={`dashboard-aircraft-${value}`} value={value}>{value}</option>
                  ))}
                </select>
              </label>
              <label>
                M1
                <select
                  value={dashboardFilters.m1Count}
                  onChange={(event) => setDashboardFilters((current) => ({ ...current, m1Count: event.target.value }))}
                >
                  <option value="">All</option>
                  {dashboardFilterOptions.m1Counts.map((value) => (
                    <option key={`dashboard-m1-${value}`} value={value}>{value}</option>
                  ))}
                </select>
              </label>
              <label>
                M2
                <select
                  value={dashboardFilters.m2Count}
                  onChange={(event) => setDashboardFilters((current) => ({ ...current, m2Count: event.target.value }))}
                >
                  <option value="">All</option>
                  {dashboardFilterOptions.m2Counts.map((value) => (
                    <option key={`dashboard-m2-${value}`} value={value}>{value}</option>
                  ))}
                </select>
              </label>
              <label>
                M3
                <select
                  value={dashboardFilters.m3Count}
                  onChange={(event) => setDashboardFilters((current) => ({ ...current, m3Count: event.target.value }))}
                >
                  <option value="">All</option>
                  {dashboardFilterOptions.m3Counts.map((value) => (
                    <option key={`dashboard-m3-${value}`} value={value}>{value}</option>
                  ))}
                </select>
              </label>
            </div>
            <div className="button-row">
              <button type="button" className="ghost-button" onClick={() => setDashboardFilters(INITIAL_DASHBOARD_FILTERS)}>
                Clear filters
              </button>
            </div>
            <div className="dashboard-export-row">
              <label className="dashboard-export-field">
                CSV file name
                <input
                  type="text"
                  value={dashboardExportFileName}
                  onChange={(event) => {
                    setDashboardExportFileName(event.target.value);
                    if (dashboardExportStatus.message) {
                      setDashboardExportStatus({ kind: 'idle', message: '' });
                    }
                  }}
                  placeholder="dashboard_export"
                />
              </label>
              <button type="button" className="primary-button" onClick={handleExportDashboardCsv}>
                Export CSV
              </button>
            </div>
            <p className="dashboard-export-note">
              Export all currently filtered rows as a semicolon-separated CSV file. Supported browsers let you choose where to save it.
            </p>
            {dashboardExportStatus.message ? (
              <p className={dashboardExportStatus.kind === 'error' ? 'error-copy' : 'status-copy'}>
                {dashboardExportStatus.message}
              </p>
            ) : null}
            <div className="dashboard-pagination">
              <span className="dashboard-pagination-summary">
                {`Page ${dashboardPage} of ${dashboardTotalPages} · ${dashboardTotalRows} total rows`}
              </span>
              <div className="dashboard-pagination-actions">
                <button
                  type="button"
                  className="ghost-button"
                  onClick={() => setDashboardPage((current) => Math.max(1, current - 1))}
                  disabled={dashboardPage <= 1}
                >
                  Previous
                </button>
                <button
                  type="button"
                  className="ghost-button"
                  onClick={() => setDashboardPage((current) => Math.min(dashboardTotalPages, current + 1))}
                  disabled={dashboardPage >= dashboardTotalPages}
                >
                  Next
                </button>
              </div>
            </div>
            <div className="dashboard-table-wrap">
              <table className="dashboard-table">
                <thead>
                  <tr>
                    <th>Run date</th>
                    <th>Run time</th>
                    <th>Game</th>
                    <th>Scenario</th>
                    <th>Status</th>
                    <th>Rounds</th>
                    <th>Aircraft</th>
                    <th>Completed missions</th>
                    <th>M1</th>
                    <th>M2</th>
                    <th>M3</th>
                    <th>Survived</th>
                    <th>Destroyed</th>
                  </tr>
                </thead>
                <tbody>
                  {dashboardPagedRows.map((row) => (
                    <tr key={row.gameAnalyticsSnapshotId}>
                      <td>{formatDateOnly(row.createdAt)}</td>
                      <td>{formatDateTime(row.createdAt)}</td>
                      <td>{row.gameName || `Game ${row.gameId}`}</td>
                      <td>{row.scenarioName}</td>
                      <td>{humanizeStatus(row.gameStatus)}</td>
                      <td>{row.roundsToOutcome}</td>
                      <td>{row.aircraftCount}</td>
                      <td>{row.completedMissionCount}/{row.missionCount}</td>
                      <td>{row.m1Count}</td>
                      <td>{row.m2Count}</td>
                      <td>{row.m3Count}</td>
                      <td>{row.survivingAircraftCount}</td>
                      <td>{row.destroyedAircraftCount}</td>
                    </tr>
                  ))}
                  {!dashboardVisibleRows.length ? (
                    <tr>
                      <td colSpan="13" className="dashboard-empty">
                        {dashboardLoading ? 'Loading analytics rows...' : 'No analytics rows match the current filter.'}
                      </td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
          </article>
        </section>
      ) : currentView === 'PLAY' ? (
        <>

      {controlCenterPanel}

      <aside className="info-panel play-color-sidebar" aria-label="Color legend">
        <button className="play-color-toggle" onClick={() => setShowColorLegend(v => !v)}>
          Aircraft colors {showColorLegend ? '▲' : '▼'}
        </button>
        {showColorLegend && (
          <div className="play-color-legend">
            <div className="play-color-legend-item"><span className="play-color-swatch play-color-blue" /> Healthy aircraft at base</div>
            <div className="play-color-legend-item"><span className="play-color-swatch play-color-orange" /> Aircraft needs repair</div>
            <div className="play-color-legend-item"><span className="play-color-swatch play-color-red" /> Destroyed aircraft</div>
            <div className="play-color-legend-item"><span className="play-color-swatch play-color-green" /> Completed mission</div>
          </div>
        )}
      </aside>

      <section className="mission-section">
        <header className="section-heading">
          <h2>Missions</h2>
          <p className="muted-copy">Available and completed mission instances for the current game.</p>
        </header>
        <div className="mission-strip">
          {missionCards.map((mission) => (
            <article key={mission.code} className={`mission-card mission-${mission.status?.toLowerCase()}`}>
              <h2>{mission.code} - {mission.name}</h2>
              <p>Flight hours: {mission.flightHours} | Fuel: {mission.fuelCost} | Weapons: {mission.weaponCost}</p>
              <strong>{humanizeStatus(mission.status)}</strong>
              {mission.blocker ? <span className="mission-blocker">{mission.blocker}</span> : null}
            </article>
          ))}
        </div>
      </section>

      <section className="holding-section">
        <header className="section-heading">
          <h2>On mission</h2>
          <p className="muted-copy">Aircraft currently flying an assigned mission. Statistics show the aircraft state before mission completion.</p>
        </header>
        <article className="holding-panel">
          {onMissionAircraft.length ? (
            <div className="holding-grid">
              {onMissionAircraft.map((aircraft) => (
                <div key={aircraft.code} className="holding-card">
                  <AircraftStatusCard aircraft={aircraft} additions={aircraftAdditionsByCode[aircraft.code]} />
                  <p className="aircraft-added-copy">Mission: {aircraft.assignedMission || 'Assigned'}</p>
                </div>
              ))}
            </div>
          ) : (
            <p className="muted-copy">No aircraft are currently on mission.</p>
          )}
        </article>
      </section>

      <section className="holding-section">
        <article className="info-panel">
          <h3>Mission complete</h3>
          <p>{lastAutoResponse?.nextAction ? `Next action - ${humanizeAction(lastAutoResponse.nextAction)}` : 'Start the next round to let the system assign missions.'}</p>
          {lastAutoResponse?.autoAssignments?.length ? (
            <ul className="compact-list">
              {lastAutoResponse.autoAssignments.map((entry) => <li key={entry}>{entry}</li>)}
            </ul>
          ) : null}
        </article>
      </section>

      <section className="holding-section">
        <header className="section-heading">
          <h2>Awaiting dice roll</h2>
          <p className="muted-copy">Aircraft that have completed their mission and are waiting for a dice outcome.</p>
        </header>
        <article className="holding-panel">
          {pendingDiceAircraft.length ? (
            <div className="holding-grid">
              {pendingDiceAircraft.map((aircraft) => (
                <div key={aircraft.code} className="holding-card">
                  <AircraftStatusCard aircraft={aircraft} additions={aircraftAdditionsByCode[aircraft.code]} />
                  <p className="aircraft-added-copy">Mission: {aircraft.assignedMission || 'Completed'}</p>
                </div>
              ))}
            </div>
          ) : (
            <p className="muted-copy">No aircraft are currently waiting for a dice roll.</p>
          )}
        </article>
      </section>

      <section className="holding-section">
        <article className="info-panel">
          <h3>Dice Outcome</h3>
          <form className="dice-form" onSubmit={handleRollDice}>
            <label>
              Aircraft
              <select value={selectedAircraft} onChange={(event) => setSelectedAircraft(event.target.value)} disabled={!canRollDice || automationEnabled}>
                {pendingDiceAircraft.length ? (
                  pendingDiceAircraft.map((aircraft) => (
                    <option key={aircraft.code} value={aircraft.code}>
                      {aircraft.code}
                    </option>
                  ))
                ) : (
                  <option value="">No pending aircraft</option>
                )}
              </select>
            </label>
            <fieldset className="radio-group">
              <legend>Dice mode</legend>
              <label className="radio-option">
                <input
                  type="radio"
                  name="diceMode"
                  checked={useRandomDice}
                  onChange={() => setUseRandomDice(true)}
                  disabled={!canRollDice || automationEnabled}
                />
                <span>Random roll</span>
              </label>
              <label className="radio-option">
                <input
                  type="radio"
                  name="diceMode"
                  checked={!useRandomDice}
                  onChange={() => setUseRandomDice(false)}
                  disabled={!canRollDice || automationEnabled}
                />
                <span>Choose outcome</span>
              </label>
              {!lastAutoResponse?.autoLandings?.length ? (
                <p className="muted-copy">Roll dice to see automatic landing decisions.</p>
              ) : null}
            </fieldset>
            {!useRandomDice ? (
              <label>
                Dice
                <input type="number" min="1" max="6" value={diceValue} onChange={(event) => setDiceValue(event.target.value)} disabled={!canRollDice || automationEnabled} />
              </label>
            ) : null}
            <div className="button-row bottom-actions bottom-actions-left">
              <button
                type="submit"
                className={`play-action-button ${nextStep === 'ROLL_DICE' ? 'next-step-button ' : ''}${canRollDice && !automationEnabled ? 'active-button ' : ''}compact-button`}
                disabled={!canRollDice || automationEnabled}
              >
                Roll dice
              </button>
            </div>
          </form>
          {automationEnabled ? <p>Automated dice handling is active.</p> : null}
          {lastAutoResponse?.autoLandings?.length ? (
            <ul className="compact-list">
              {lastAutoResponse.autoLandings.map((entry) => <li key={entry}>{entry}</li>)}
            </ul>
          ) : null}
        </article>
      </section>

      <section className="holding-section">
        <header className="section-heading">
          <h2>Holding</h2>
          <p className="muted-copy">Aircraft that could not land and are currently circling in the air.</p>
        </header>
        <article className="holding-panel">
          {holdingAircraft.length ? (
            <div className="holding-grid">
              {holdingAircraft.map((aircraft) => (
                <div key={aircraft.code} className="holding-card">
                  <AircraftStatusCard aircraft={aircraft} additions={aircraftAdditionsByCode[aircraft.code]} />
                </div>
              ))}
            </div>
          ) : (
            <p className="muted-copy">No aircraft are currently in holding.</p>
          )}
        </article>
      </section>

      <section className="holding-section">
        <header className="section-heading">
          <h2>Destroyed aircraft</h2>
          <p className="muted-copy">Aircraft that have crashed or been lost during the game.</p>
        </header>
        <article className="holding-panel">
          {destroyedAircraft.length ? (
            <div className="holding-grid">
              {destroyedAircraft.map((aircraft) => (
                <div key={aircraft.code} className="holding-card holding-card-destroyed">
                  <AircraftStatusCard aircraft={aircraft} additions={aircraftAdditionsByCode[aircraft.code]} />
                </div>
              ))}
            </div>
          ) : (
            <p className="muted-copy">No aircraft have been destroyed.</p>
          )}
        </article>
      </section>

      <section className="bases-section">
        <header className="section-heading">
          <h2>Bases</h2>
          <p className="muted-copy">Current base inventory, parking slots, and maintenance capacity.</p>
        </header>
        <div className="bases-grid">
          {basesWithAircraft.map((base) => (
            <article key={base.code} className="base-card">
              <header className="base-header">{base.name}</header>
              <p className="muted-copy">
                Max fuel {baseReferenceByCode[normalizeBaseCode(base.code)]?.maxInventory?.fuel ?? 0} | Max weapons {baseReferenceByCode[normalizeBaseCode(base.code)]?.maxInventory?.weapons ?? 0} | Max spare parts {baseReferenceByCode[normalizeBaseCode(base.code)]?.maxInventory?.spareParts ?? 0}
              </p>
              <div className="warehouse-panel">
                <h3>Deliveries</h3>
                {deliverySummaryForBase(base.code, gameState?.game?.currentRound ?? 0, rules?.resourceRules).map((entry) => (
                  <p key={`${base.code}-${entry.resource}`}>{entry.resourceLabel} {entry.amount > 0 ? `+${entry.amount}` : entry.amount} {entry.roundsText}</p>
                ))}
              </div>
              <div className="warehouse-panel">
                <h3>Warehouse</h3>
                <p>Fuel {base.fuelStock}</p>
                <p>Weapons {base.weaponsStock}</p>
                <p>Reserveparts {base.sparePartsStock}</p>
                <p>Parking slots free {Math.max(0, base.parkingCapacity - base.parked.length)} / {base.parkingCapacity}</p>
                <p>Repair slots free {Math.max(0, base.maintenanceCapacity - base.maintenance.length)} / {base.maintenanceCapacity}</p>
              </div>
              <div className="slot-grid">
                <div className="slot-panel">
                  <h3>Park</h3>
                  <div className="slots">
                    {Array.from({ length: base.parkingCapacity }).map((_, index) => {
                      const aircraft = base.parked[index];
                      const needsRepair = aircraftNeedsRepair(aircraft);
                      const slotLabel = `Slot ${index + 1}`;
                      return (
                        <div
                          key={`${base.code}-park-${index}`}
                          className={`slot slot-${aircraft ? 'filled' : 'empty'}${needsRepair ? ' slot-needs-repair' : ''}`}
                        >
                          {aircraft ? (
                            <WarehouseAircraftTile
                              aircraft={aircraft}
                              additions={aircraftAdditionsByCode[aircraft.code]}
                              slotLabel={slotLabel}
                              mode={needsRepair ? 'repair' : 'park'}
                            />
                          ) : (
                            <WarehouseEmptySlot slotLabel={slotLabel} />
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
                {base.maintenanceCapacity > 0 ? (
                  <div className="slot-panel">
                    <h3>Repair</h3>
                    <div className="slots">
                      {Array.from({ length: base.maintenanceCapacity }).map((_, index) => {
                        const aircraft = base.maintenance[index];
                        const slotLabel = `Slot ${index + 1}`;
                        return (
                          <div key={`${base.code}-repair-${index}`} className={`slot slot-${aircraft ? 'repairing' : 'empty'}${aircraft ? ' slot-needs-repair' : ''}`}>
                            {aircraft ? (
                              <WarehouseAircraftTile
                                aircraft={aircraft}
                                additions={aircraftAdditionsByCode[aircraft.code]}
                                slotLabel={slotLabel}
                                mode="repair"
                              />
                            ) : (
                              <WarehouseEmptySlot slotLabel={slotLabel} />
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                ) : null}
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="bottom-panels">
        <section className="event-panel">
          <h3>Event history</h3>
          {eventLog.length ? (
            <div className="event-list">
              {eventLog.map((entry) => (
                <article key={entry.id} className="event-item">
                  <strong>{entry.title}</strong>
                  <span>{eventMeta(entry)}</span>
                  {renderEventDetails(entry, rules)}
                  {entry.payload?.messages?.length ? <p>{entry.payload.messages.join(' ')}</p> : null}
                </article>
              ))}
            </div>
          ) : (
            <p className="muted-copy">Game started events and autoplay decisions will appear here.</p>
          )}
        </section>
        <article className="info-panel analysis-feed-panel">
          <div className="analysis-feed-header">
            <h3>Analysis feed</h3>
            {analysisPending ? <span className="analysis-pending">Pending...</span> : null}
          </div>
          {postGameSummary?.snapshot ? (
            <div className="post-game-summary">
              <div className={`post-game-outcome post-game-outcome-${postGameSummary.snapshot.isWin ? 'won' : 'lost'}`}>
                {postGameSummary.snapshot.isWin ? 'Mission accomplished' : 'Mission failed'} — {postGameSummary.snapshot.gameStatus}
              </div>
              <div className="post-game-stats">
                <div className="post-game-stat"><span>Missions</span><strong>{postGameSummary.snapshot.completedMissionCount ?? '—'}/{postGameSummary.snapshot.missionCount ?? '—'}</strong></div>
                <div className="post-game-stat"><span>Rounds</span><strong>{postGameSummary.snapshot.roundsToOutcome ?? '—'}</strong></div>
                <div className="post-game-stat"><span>Survived</span><strong>{postGameSummary.snapshot.survivingAircraftCount ?? '—'}/{postGameSummary.snapshot.aircraftCount ?? '—'}</strong></div>
                <div className="post-game-stat"><span>Lost</span><strong>{postGameSummary.snapshot.destroyedAircraftCount ?? '—'}</strong></div>
              </div>
              {(postGameSummary.finalFeed || []).map((item) => (
                <article key={item.id} className="event-item analysis-feed-item post-game-feed-item">
                  <div className="analysis-feed-meta">
                    <strong>{item.role}</strong>
                    <span className={`analysis-source-badge analysis-source-${String(item.source || 'rule-based').toLowerCase()}`}>{item.source || 'Rule-based'}</span>
                  </div>
                  <p>{item.summary}</p>
                  {item.details ? <p className="muted-copy">{item.details}</p> : null}
                </article>
              ))}
              {analysisFeed.filter((item) => item.round > 0).length > 0 ? (
                <details className="post-game-round-feed">
                  <summary>Round-by-round feed ({analysisFeed.filter((item) => item.round > 0).length} entries)</summary>
                  <div ref={analysisFeedListRef} className="event-list analysis-feed-list">
                    {analysisFeed.filter((item) => item.round > 0).map((item) => (
                      <article key={item.id} className="event-item analysis-feed-item">
                        <div className="analysis-feed-meta">
                          <strong>{item.role}</strong>
                          <div className="analysis-feed-meta-right">
                            <span className={`analysis-source-badge analysis-source-${String(item.source || 'rule-based').toLowerCase()}`}>{item.source || 'Rule-based'}</span>
                            <span>Round {item.round}</span>
                          </div>
                        </div>
                        <p>{item.summary}</p>
                        {item.details ? <p className="muted-copy">{item.details}</p> : null}
                      </article>
                    ))}
                  </div>
                </details>
              ) : null}
            </div>
          ) : analysisFeed.length ? (
            <div ref={analysisFeedListRef} className="event-list analysis-feed-list">
              {analysisFeed.map((item) => (
                <article key={item.id} className="event-item analysis-feed-item">
                  <div className="analysis-feed-meta">
                    <strong>{item.role}</strong>
                    <div className="analysis-feed-meta-right">
                      <span className={`analysis-source-badge analysis-source-${String(item.source || 'rule-based').toLowerCase()}`}>{item.source || 'Rule-based'}</span>
                      <span>Round {item.round}</span>
                    </div>
                  </div>
                  <p>{item.summary}</p>
                  {item.details ? <p className="muted-copy">{item.details}</p> : null}
                </article>
              ))}
            </div>
          ) : (
            <div className="analysis-feed-empty">
              <p className="muted-copy">Round analysis will appear here as a running feed.</p>
            </div>
          )}
        </article>
      </section>
        </>
      ) : null}
    </main>
  );
}

function formatElapsedTime(totalSeconds) {
  const seconds = Math.max(0, Number(totalSeconds) || 0);
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const remainingSeconds = seconds % 60;
  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, '0')}:${String(remainingSeconds).padStart(2, '0')}`;
  }
  return `${minutes}:${String(remainingSeconds).padStart(2, '0')}`;
}

function formatDateTime(value) {
  if (!value) {
    return 'N/A';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }
  return date.toLocaleString('sv-SE', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

function formatDateOnly(value) {
  if (!value) {
    return 'N/A';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }
  return date.toLocaleDateString('sv-SE', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
}

function normalizeDashboardExportFileName(value) {
  return String(value || '')
    .trim()
    .replace(/\.csv$/i, '')
    .replace(/[\\/:*?"<>|]/g, '_');
}

function buildDashboardCsv(rows) {
  const headers = [
    'run_date',
    'run_time',
    'game_name',
    'scenario_name',
    'status',
    'rounds',
    'aircraft_count',
    'completed_missions',
    'mission_count',
    'm1_count',
    'm2_count',
    'm3_count',
    'surviving_aircraft_count',
    'destroyed_aircraft_count',
    'dice_selection_profile',
  ];

  const lines = rows.map((row) => ([
    formatDateOnly(row.createdAt),
    formatDateTime(row.createdAt),
    row.gameName || `Game ${row.gameId}`,
    row.scenarioName,
    row.gameStatus,
    row.roundsToOutcome,
    row.aircraftCount,
    row.completedMissionCount,
    row.missionCount,
    row.m1Count,
    row.m2Count,
    row.m3Count,
    row.survivingAircraftCount,
    row.destroyedAircraftCount,
    row.diceSelectionProfile || '',
  ]).map(csvEscape).join(';'));

  return `${headers.join(';')}\n${lines.join('\n')}`;
}

function csvEscape(value) {
  const text = String(value ?? '');
  if (!/[;"\n]/.test(text)) {
    return text;
  }
  return `"${text.replace(/"/g, '""')}"`;
}

function MetricCard({ label, value }) {
  return (
    <article className="metric-card">
      <span>{label}</span>
      {typeof value === 'string' || typeof value === 'number' ? <strong>{value}</strong> : <div className="metric-card-rich-value">{value}</div>}
    </article>
  );
}

function WarehouseAircraftTile({ aircraft, additions, slotLabel, mode }) {
  const repairRequired = mode === 'repair' || aircraftNeedsRepair(aircraft);
  const theme = warehouseFlightTheme(aircraft, mode);
  const titleParts = [
    aircraft?.code || 'Unknown aircraft',
    `Fuel ${aircraft?.fuel ?? 0}/100`,
    `Weapons ${aircraft?.weapons ?? 0}/6`,
    `Flight hours ${aircraft?.remainingFlightHours ?? 0}/20`,
    aircraft?.damage && aircraft.damage !== 'NONE' ? `Repair ${humanizeStatus(aircraft.damage)}` : 'Repair none',
    additions?.fuel ? `Fuel +${additions.fuel}` : null,
    additions?.weapons ? `Weapons +${additions.weapons}` : null,
    additions?.hours ? `Flight hours +${additions.hours}` : null,
  ].filter(Boolean);

  return (
    <div
      className={`warehouse-flight-tile${repairRequired ? ' warehouse-flight-tile-repair' : ''}`}
      style={{
        '--warehouse-flight-bg': theme.background,
        '--warehouse-flight-border': theme.border,
        '--warehouse-flight-text': theme.text,
        '--warehouse-flight-shadow': theme.shadow,
        '--warehouse-flight-badge-bg': theme.badgeBackground,
        '--warehouse-flight-badge-border': theme.badgeBorder,
        '--warehouse-flight-badge-text': theme.badgeText,
        '--warehouse-flight-glow': theme.glow,
      }}
      title={titleParts.join(' | ')}
    >
      <div className="warehouse-flight-topline">
        <span className="warehouse-flight-slot">{slotLabel}</span>
        <span className="warehouse-flight-status">{repairRequired ? 'Repair' : 'Ready'}</span>
      </div>
      <div className="warehouse-flight-visual">
        <img className="warehouse-flight-image" src={GRIPEN_IMAGE_URL} alt={`Gripen aircraft ${aircraft.code}`} />
      </div>
      <div className="warehouse-flight-copy">
        <strong>{aircraft.code}</strong>
        <span>{repairRequired ? humanizeStatus(aircraft.damage || aircraft.status) : 'Flight ID'}</span>
      </div>
    </div>
  );
}

function WarehouseEmptySlot({ slotLabel }) {
  return (
    <div className="warehouse-empty-slot">
      <span className="warehouse-empty-slot-label">{slotLabel}</span>
      <span className="warehouse-empty-slot-copy">Available</span>
    </div>
  );
}

function AircraftStatusCard({ aircraft, additions, compact = false }) {
  const maxStats = {
    fuel: 100,
    weapons: 6,
    hours: 20,
  };
  const isDestroyed = aircraft?.status === 'CRASHED' || aircraft?.status === 'DESTROYED';
  const positiveAdditions = [
    additions?.fuel ? `Fuel +${additions.fuel}` : null,
    additions?.weapons ? `Weapons +${additions.weapons}` : null,
    additions?.hours ? `Flight hours +${additions.hours}` : null,
  ].filter(Boolean);

  return (
    <div className={`aircraft-status-card${compact ? ' aircraft-status-card-compact' : ''}${isDestroyed ? ' aircraft-status-card-destroyed' : ''}`}>
      <strong>{aircraft.code}</strong>
      <div className="aircraft-status-card-details">
        <ul className="aircraft-stats-list">
          <li>Fuel {aircraft.fuel}/{maxStats.fuel}</li>
          <li>Weapons {aircraft.weapons}/{maxStats.weapons}</li>
          <li>Flight hours {aircraft.remainingFlightHours}/{maxStats.hours}</li>
          <li>{aircraft.damage === 'NONE' ? 'Repair none' : `Repair ${humanizeStatus(aircraft.damage)}`}</li>
        </ul>
        {positiveAdditions.length ? <p className="aircraft-added-copy">Added: {positiveAdditions.join(', ')}</p> : null}
      </div>
    </div>
  );
}

function completedMissionCount(gameState) {
  return (gameState?.missions || []).filter((mission) => mission.status === 'COMPLETED').length;
}

function holdingCount(gameState) {
  return (gameState?.aircraft || []).filter((aircraft) => aircraft.status === 'HOLDING').length;
}

function crashedCount(gameState) {
  return (gameState?.aircraft || []).filter((aircraft) => aircraft.status === 'CRASHED' || aircraft.status === 'DESTROYED').length;
}

function humanizeStatus(status) {
  if (!status) {
    return 'Unknown';
  }
  return status
    .toString()
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/^\w/, (character) => character.toUpperCase());
}

function humanizeAction(action) {
  if (!action) {
    return 'Wait';
  }
  return humanizeStatus(action);
}

export default App;

function isValidGameId(value) {
  if (value === null || value === undefined) {
    return false;
  }
  const normalized = String(value).trim();
  return normalized !== '' && normalized !== 'undefined' && /^\d+$/.test(normalized);
}

function totalConfiguredMissionCount(missionTypeCounts) {
  return Object.values(missionTypeCounts || {}).reduce((total, count) => total + Number(count || 0), 0);
}

function finalGameMessage(gameState) {
  const summary = summarizeGameOutcome(gameState);
  const gameName = gameState?.game?.name;
  const prefix = gameName ? `Game ${gameName}` : 'Game';
  const timeoutReason = isMaxRoundsFailure(gameState)
    ? ' Result: failed because the game exceeded the configured maximum number of rounds.'
    : '';
  return `${prefix} finished with status ${gameState?.game?.status}.${timeoutReason} ${summary}`;
}

function finalGameStatusKind(gameState, defaultKind = 'success') {
  return isMaxRoundsFailure(gameState) ? 'error' : defaultKind;
}

function defaultDuplicateScenarioName(name) {
  return normalizeScenarioTemplateName(name ? `${name}_COPY` : 'SCENARIO_COPY');
}

function defaultSimulationBatchName(name) {
  return normalizeScenarioTemplateName(name ? `${name}_SIM` : 'SIM_BATCH');
}

function normalizeScenarioTemplateName(value) {
  return (value || '')
    .toUpperCase()
    .replace(/[^A-Z0-9_]/g, '');
}

function finalGameLogTitle(gameState) {
  if (gameState?.game?.status === 'WON') {
    return 'Game won';
  }
  if (gameState?.game?.status === 'ABORTED') {
    return 'Game aborted';
  }
  if (isMaxRoundsFailure(gameState)) {
    return 'Game failed on max rounds';
  }
  return 'Game lost';
}

function enrichGameFinishedPayload(payload) {
  if (!payload?.gameFinished || !payload?.gameState) {
    return payload;
  }
  const summary = summarizeGameOutcome(payload.gameState);
  return {
    ...payload,
    messages: [...(payload.messages || []), summary],
  };
}

function summarizeGameOutcome(gameState) {
  const completedMissions = completedMissionCount(gameState);
  const totalMissions = gameState?.missions?.length || 0;
  const landedAircraft = (gameState?.aircraft || []).filter((aircraft) => aircraft.currentBase && aircraft.status === 'READY').length;
  const destroyedAircraft = crashedCount(gameState);
  const rounds = gameState?.game?.currentRound ?? 0;
  return `${completedMissions}/${totalMissions} missions completed, ${landedAircraft} aircraft landed, ${destroyedAircraft} destroyed, ${rounds} rounds.`;
}

function isMaxRoundsFailure(gameState) {
  const status = gameState?.game?.status;
  const currentRound = Number(gameState?.game?.currentRound ?? 0);
  const maxRounds = Number(gameState?.game?.maxRounds ?? 0);
  return status === 'LOST' && maxRounds > 0 && currentRound >= maxRounds;
}

function scenarioRulesFor(selectedScenario, scenarioName) {
  if (!selectedScenario) {
    return {
      title: scenarioName || 'Scenario',
      summary: 'No scenario description is available for this setup yet.',
      points: [],
    };
  }

  const aircraft = selectedScenario.aircraft || [];
  const missions = selectedScenario.missions || [];
  const bases = selectedScenario.bases || [];
  const diceRules = selectedScenario.diceRules || [];
  const totalParking = bases.reduce((sum, base) => sum + Number(base.parkingCapacity || 0), 0);
  const totalRepair = bases.reduce((sum, base) => sum + Number(base.maintenanceCapacity || 0), 0);
  const groupedAircraft = Object.values(aircraft.reduce((accumulator, item) => {
    const key = item.aircraftTypeCode || item.code;
    accumulator[key] = accumulator[key] || [];
    accumulator[key].push(item);
    return accumulator;
  }, {}));

  const aircraftPoint = groupedAircraft.length
    ? `Aircraft start with ${groupedAircraft.map((group) => {
        const sample = group[0];
        return `${sample.aircraftTypeCode} x${group.length} at Fuel ${sample.fuelStart}, Weapons ${sample.weaponsStart}, and Flight Hours ${sample.flightHoursStart}`;
      }).join('; ')}.`
    : null;

  const missionPoint = missions.length
    ? `Mission costs: ${missions.map((mission) => (
        `${mission.missionTypeCode} ${mission.missionTypeName} ${mission.fuelCost} fuel / ${mission.weaponCost} weapons / ${mission.flightTimeCost} hours`
      )).join(', ')}.`
    : null;

  const capacityPoint = bases.length
    ? `Total base capacity in this scenario is ${totalParking} parking slots and ${totalRepair} maintenance slots.`
    : null;

  const deliveryPoints = ['FUEL', 'SPARE_PARTS', 'WEAPONS']
    .map((resource) => {
      const deliveries = bases.map((base) => {
        const match = (base.supplyRules || []).find((rule) => rule.resource === resource);
        if (!match || Number(match.deliveryAmount || 0) <= 0) {
          return null;
        }
        return `${base.code} +${match.deliveryAmount} every ${match.frequencyRounds} rounds`;
      }).filter(Boolean);
      if (!deliveries.length) {
        return null;
      }
      return `${humanizeStatus(resource)} deliveries: ${deliveries.join(', ')}.`;
    })
    .filter(Boolean);

  const dicePoint = diceRules.length
    ? `Dice outcomes: ${diceRules.map((rule) => (
        `${rule.diceValue} ${humanizeStatus(rule.damageType)}`
      )).join(', ')}.`
    : null;

  return {
    title: selectedScenario.name || scenarioName || 'Scenario',
    summary: selectedScenario.description || 'Scenario configuration generated from the selected setup.',
    points: [
      aircraftPoint,
      missionPoint,
      capacityPoint,
      ...deliveryPoints,
      dicePoint,
      'Some rounds may have no available actions and can be passed directly to the next round.',
    ].filter(Boolean),
  };
}

function normalizeBaseCode(code) {
  if (!code) {
    return '';
  }
  return String(code).replace(/^BASE_/, '');
}

function deliverySummaryForBase(baseCode, currentRound, resourceRules) {
  if (!resourceRules) {
    return [];
  }

  const normalizedBaseCode = normalizeBaseCode(baseCode);
  return [
    buildDeliveryEntry('Fuel', normalizedBaseCode, currentRound, resourceRules.fuelDeliveries),
    buildDeliveryEntry('Weapons', normalizedBaseCode, currentRound, resourceRules.weaponDeliveries),
    buildDeliveryEntry('Spare parts', normalizedBaseCode, currentRound, resourceRules.sparePartDeliveries),
  ].filter(Boolean);
}

function buildDeliveryEntry(resourceLabel, baseCode, currentRound, schedule) {
  if (!schedule?.deliveries) {
    return null;
  }
  const amount = Number(schedule.deliveries[baseCode] ?? 0);
  if (amount <= 0) {
    return null;
  }

  const frequency = Number(schedule.frequencyRounds || 0);
  if (frequency <= 0) {
    return null;
  }

  const nextRound = currentRound + 1;
  const roundsUntilDelivery = (frequency - (nextRound % frequency)) % frequency;

  return {
    resource: resourceLabel.toLowerCase(),
    resourceLabel,
    amount,
    roundsText: roundsUntilDelivery === 0 ? 'next round' : `in ${roundsUntilDelivery + 1} rounds`,
  };
}

function randomDiceValue() {
  return Math.floor(Math.random() * 6) + 1;
}

function automatedDiceValue(strategy) {
  if (strategy === 'MIN_DAMAGE') {
    return [4, 5, 6][Math.floor(Math.random() * 3)];
  }
  if (strategy === 'MAX_DAMAGE') {
    return [1, 2, 3][Math.floor(Math.random() * 3)];
  }
  return randomDiceValue();
}

function buildMissionPreviewState(currentState, autoAssignments) {
  if (!currentState?.aircraft?.length || !autoAssignments?.length) {
    return null;
  }

  const assignmentsByAircraft = Object.fromEntries(
    autoAssignments
      .map((entry) => String(entry).split(' -> '))
      .filter((parts) => parts.length === 2)
  );

  const previewAircraft = currentState.aircraft.map((aircraft) => {
    const missionCode = assignmentsByAircraft[aircraft.code];
    if (!missionCode) {
      return aircraft;
    }
    return {
      ...aircraft,
      status: 'ON_MISSION',
      assignedMission: missionCode,
    };
  });

  if (!previewAircraft.some((aircraft) => aircraft.status === 'ON_MISSION')) {
    return null;
  }

  const previewMissions = (currentState.missions || []).map((mission) => {
    const missionAssigned = Object.values(assignmentsByAircraft).includes(mission.code);
    return missionAssigned ? { ...mission, status: 'ASSIGNED', assignmentBlocker: null } : mission;
  });

  return {
    ...currentState,
    aircraft: previewAircraft,
    missions: previewMissions,
  };
}

function aircraftNeedsRepair(aircraft) {
  if (!aircraft) {
    return false;
  }
  return aircraft.status === 'IN_MAINTENANCE'
    || aircraft.status === 'WAITING_MAINTENANCE'
    || (aircraft.damage && aircraft.damage !== 'NONE');
}

function extractMissionAssignments(autoAssignments) {
  return Object.fromEntries(
    (autoAssignments || [])
      .map((entry) => String(entry).split(' -> '))
      .filter((parts) => parts.length === 2)
  );
}

function warehouseFlightTheme(aircraft, mode) {
  const repairRequired = mode === 'repair' || aircraftNeedsRepair(aircraft);
  const status = aircraft?.status;

  if (status === 'CRASHED' || status === 'DESTROYED') {
    return {
      background: 'var(--color-danger-bg)',
      border: 'var(--color-danger-bdr)',
      text: 'var(--color-danger)',
      shadow: 'rgba(220, 38, 38, 0.18)',
      badgeBackground: 'rgba(220, 38, 38, 0.12)',
      badgeBorder: 'rgba(220, 38, 38, 0.24)',
      badgeText: 'var(--color-danger)',
      glow: 'rgba(220, 38, 38, 0.08)',
    };
  }

  if (repairRequired) {
    return {
      background: 'var(--color-warning-bg)',
      border: 'var(--color-warning-bdr)',
      text: 'var(--color-warning)',
      shadow: 'rgba(217, 119, 6, 0.16)',
      badgeBackground: 'rgba(217, 119, 6, 0.12)',
      badgeBorder: 'rgba(217, 119, 6, 0.22)',
      badgeText: 'var(--color-warning)',
      glow: 'rgba(217, 119, 6, 0.08)',
    };
  }

  if (status === 'COMPLETED') {
    return {
      background: 'var(--color-success)',
      border: 'rgba(22, 163, 74, 0.35)',
      text: '#f0fdf4',
      shadow: 'rgba(22, 163, 74, 0.18)',
      badgeBackground: 'rgba(240, 253, 244, 0.12)',
      badgeBorder: 'rgba(240, 253, 244, 0.2)',
      badgeText: '#f0fdf4',
      glow: 'rgba(240, 253, 244, 0.12)',
    };
  }

  return {
    background: 'var(--color-primary)',
    border: 'rgba(191, 219, 254, 0.18)',
    text: 'var(--color-primary-text)',
    shadow: 'rgba(30, 58, 95, 0.22)',
    badgeBackground: 'rgba(255, 255, 255, 0.12)',
    badgeBorder: 'rgba(255, 255, 255, 0.2)',
    badgeText: 'var(--color-primary-text)',
    glow: 'rgba(255, 255, 255, 0.12)',
  };
}

function renderEventDetails(entry, rules) {
  const diceRoll = entry.payload?.diceRoll;
  if (!diceRoll) {
    return null;
  }

  const outcomeRule = rules?.diceOutcomes?.find((outcome) => outcome.diceValue === Number(diceRoll.diceValue));
  const aircraftState = entry.payload?.gameState?.aircraft?.find((aircraft) => aircraft.code === diceRoll.aircraftCode);
  const details = [
    `Dice: ${diceRoll.diceValue}`,
    outcomeRule?.outcome ? `Meaning: ${outcomeRule.outcome}` : null,
    aircraftState?.damage && aircraftState.damage !== 'NONE' ? `Damage: ${humanizeStatus(aircraftState.damage)}` : null,
    outcomeRule?.sparePartsCost ? `Spare parts: ${outcomeRule.sparePartsCost}` : null,
    outcomeRule?.repairRounds ? `Repair rounds: ${outcomeRule.repairRounds}` : null,
  ].filter(Boolean);

  return details.length ? <p>{details.join(' | ')}</p> : null;
}

function eventMeta(entry) {
  const round = entry?.payload?.gameState?.game?.currentRound
    ?? entry?.payload?.currentRound
    ?? entry?.payload?.roundNumber;
  return round === undefined || round === null
    ? `Time: ${entry.stamp}`
    : `Time: ${entry.stamp} | Round: ${round}`;
}
