import './App.css';
import { useEffect, useMemo, useState } from 'react';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';

const INITIAL_CREATE_FORM = {
  scenarioName: 'SCN_STANDARD',
  version: '7',
  aircraftCount: 3,
  missionTypeCounts: {
    M1: 1,
    M2: 1,
    M3: 1,
  },
};

function App() {
  const [createForm, setCreateForm] = useState(INITIAL_CREATE_FORM);
  const [gameId, setGameId] = useState('');
  const [rules, setRules] = useState(null);
  const [showScenarioRules, setShowScenarioRules] = useState(false);
  const [previousGameState, setPreviousGameState] = useState(null);
  const [gameState, setGameState] = useState(null);
  const [lastAutoResponse, setLastAutoResponse] = useState(null);
  const [selectedAircraft, setSelectedAircraft] = useState('');
  const [diceValue, setDiceValue] = useState(1);
  const [useRandomDice, setUseRandomDice] = useState(true);
  const [eventLog, setEventLog] = useState([]);
  const [status, setStatus] = useState({ kind: 'idle', message: 'Create a game to begin.' });

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

  const nextStep = useMemo(() => {
    if (!isValidGameId(gameId) || !gameState) {
      return 'CREATE_GAME';
    }
    if (gameState?.game?.status && gameState.game.status !== 'ACTIVE') {
      return 'NONE';
    }
    if (pendingDiceAircraft.length) {
      return 'ROLL_DICE';
    }
    if (gameState?.game?.canStartRound || lastAutoResponse?.nextAction === 'START_NEXT_ROUND') {
      return 'NEXT_TURN';
    }
    return 'NONE';
  }, [gameId, gameState, lastAutoResponse, pendingDiceAircraft]);

  const canStartNextTurn = nextStep === 'NEXT_TURN';
  const canRollDice = nextStep === 'ROLL_DICE';
  const selectedScenarioRules = scenarioRulesFor(createForm.scenarioName);

  useEffect(() => {
    if (!pendingDiceAircraft.length) {
      setSelectedAircraft('');
      return;
    }
    if (!pendingDiceAircraft.some((aircraft) => aircraft.code === selectedAircraft)) {
      setSelectedAircraft(pendingDiceAircraft[0].code);
    }
  }, [pendingDiceAircraft, selectedAircraft]);

  const basesWithAircraft = useMemo(() => {
    const baseSource = gameState?.bases?.length
      ? gameState.bases
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
  }, [gameState, rules]);

  const baseReferenceByCode = useMemo(() => {
    return Object.fromEntries((rules?.bases || []).map((base) => [normalizeBaseCode(base.code), base]));
  }, [rules]);

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

async function request(path, options = {}) {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      headers: {
        'Content-Type': 'application/json',
      },
      ...options,
    });

    const text = await response.text();
    const data = text ? JSON.parse(text) : null;

    if (!response.ok) {
      throw new Error(data?.message || text || `Request failed with ${response.status}`);
    }

    return data;
  }

  useEffect(() => {
    let ignore = false;

    async function loadRules() {
      try {
        const data = await request('/reference/rules');
        if (!ignore) {
          setRules(data);
        }
      } catch (error) {
        if (!ignore) {
          setStatus({ kind: 'error', message: error.message });
        }
      }
    }

    loadRules();
    return () => {
      ignore = true;
    };
  }, []);

  function pushLog(title, payload) {
    const stamp = new Date().toLocaleTimeString('en-GB');
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
    setPreviousGameState(gameState);
    setGameState(nextState);
  }

  async function handleCreateGame(event) {
    event.preventDefault();
    await createNewGame('Creating game...', 'Game created');
  }

  async function handleResetGame() {
    await createNewGame('Resetting game...', 'Game reset');
  }

  async function createNewGame(loadingMessage, successPrefix) {
    setStatus({ kind: 'loading', message: loadingMessage });
    try {
      const data = await request('/games', {
        method: 'POST',
        body: JSON.stringify(createForm),
      });
      if (!isValidGameId(data?.gameId)) {
        throw new Error('Create game did not return a valid gameId.');
      }
      const nextGameId = String(data.gameId);
      setGameId(nextGameId);
      setLastAutoResponse(null);
      setSelectedAircraft('');
      setDiceValue(1);
      setUseRandomDice(true);
      setEventLog([]);
      setPreviousGameState(null);
      await refreshGameState(nextGameId);
      setStatus({ kind: 'success', message: `${successPrefix} ${nextGameId}.` });
      pushLog(successPrefix, data);
    } catch (error) {
      setStatus({ kind: 'error', message: error.message });
    }
  }

  async function handleNextRound() {
    if (!gameId) {
      setStatus({ kind: 'error', message: 'Create a game first.' });
      return;
    }
    if (!isValidGameId(gameId)) {
      setStatus({ kind: 'error', message: 'Game ID is invalid. Create a new game first.' });
      return;
    }
    setStatus({ kind: 'loading', message: 'Starting next round and assigning missions...' });
    try {
      const data = await request(`/games/${gameId}/rounds/next`, { method: 'POST' });
      setLastAutoResponse(data);
      applyGameState(data.gameState);
      setStatus({
        kind: 'success',
        message: data.gameFinished
          ? `Game finished with status ${data.gameState.game.status}.`
          : `Round prepared. Next action: ${humanizeAction(data.nextAction)}.`,
      });
      pushLog('Autoplay round start', data);
    } catch (error) {
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
    setStatus({ kind: 'loading', message: `Submitting dice roll for ${selectedAircraft}...` });
    try {
      const resolvedDiceValue = useRandomDice ? randomDiceValue() : Number(diceValue);
      const data = await request(`/games/${gameId}/dice-rolls/auto`, {
        method: 'POST',
        body: JSON.stringify({
          aircraftCode: selectedAircraft,
          diceValue: resolvedDiceValue,
        }),
      });
      setLastAutoResponse(data);
      applyGameState(data.gameState);
      setStatus({
        kind: data.gameFinished ? 'success' : 'idle',
        message: data.gameFinished
          ? `Game finished with status ${data.gameState.game.status}.`
          : `Dice resolved. Next action: ${humanizeAction(data.nextAction)}.`,
      });
      pushLog(`Dice submitted for ${selectedAircraft}`, {
        ...data,
        diceRoll: {
          aircraftCode: selectedAircraft,
          diceValue: resolvedDiceValue,
        },
      });
    } catch (error) {
      setStatus({ kind: 'error', message: error.message });
    }
  }

  return (
    <main className="shell">
      <section className="topbar">
        <div className="brand-card brand-card-wide">Smart Air Base</div>
        <div className="headline-divider" />
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
      </section>

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

      <section className="bases-section">
        <header className="section-heading">
          <h2>Bases</h2>
          <p className="muted-copy">Current base inventory, parking slots, and maintenance capacity.</p>
        </header>
        <div className="bases-layout">
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
                        return (
                          <div key={`${base.code}-park-${index}`} className={`slot slot-${aircraft ? 'filled' : 'empty'}`}>
                            {aircraft ? <AircraftStatusCard aircraft={aircraft} additions={aircraftAdditionsByCode[aircraft.code]} compact /> : `Slot ${index + 1}`}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                  <div className="slot-panel">
                    <h3>Repair</h3>
                    <div className="slots">
                      {Array.from({ length: base.maintenanceCapacity }).map((_, index) => {
                        const aircraft = base.maintenance[index];
                        return (
                          <div key={`${base.code}-repair-${index}`} className={`slot slot-${aircraft ? 'repairing' : 'empty'}`}>
                            {aircraft ? <AircraftStatusCard aircraft={aircraft} additions={aircraftAdditionsByCode[aircraft.code]} compact /> : `Slot ${index + 1}`}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </div>
              </article>
            ))}
          </div>

          <aside className="side-column">
            <section className="event-panel">
              <h3>Control center</h3>
              <form className="create-form" onSubmit={handleCreateGame}>
              <label>
                Scenario
                <select
                  value={createForm.scenarioName}
                  onChange={(event) => setCreateForm((current) => ({ ...current, scenarioName: event.target.value }))}
                >
                  <option value="SCN_STANDARD">SCN_STANDARD</option>
                </select>
              </label>
              <button type="button" className="ghost-button" onClick={() => setShowScenarioRules((current) => !current)}>
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
                <button type="submit" className={nextStep === 'CREATE_GAME' ? 'next-step-button' : ''}>Create game</button>
              </form>
              <label>
                Game ID
                <input value={gameId} onChange={(event) => setGameId(event.target.value)} placeholder="Game id" />
              </label>
              <div className="button-row">
                <button type="button" className={nextStep === 'NEXT_TURN' ? 'next-step-button' : ''} onClick={handleNextRound} disabled={!canStartNextTurn}>Next turn</button>
                <button type="button" className="ghost-button" onClick={() => refreshGameState().then((data) => data && pushLog('State refreshed', data)).catch((error) => setStatus({ kind: 'error', message: error.message }))}>Refresh</button>
              </div>
              <p className={`status-pill status-${status.kind}`}>{status.message}</p>
          </section>

          <section className="event-panel">
            <h3>Event history</h3>
            {eventLog.length ? (
              <div className="event-list">
                {eventLog.map((entry) => (
                  <article key={entry.id} className="event-item">
                    <strong>{entry.title}</strong>
                    <span>{entry.stamp}</span>
                    {renderEventDetails(entry, rules)}
                    {entry.payload?.messages?.length ? <p>{entry.payload.messages.join(' ')}</p> : null}
                  </article>
                ))}
                </div>
              ) : (
                <p className="muted-copy">Game started events and autoplay decisions will appear here.</p>
              )}
            </section>
          </aside>
        </div>
      </section>

      <section className="bottom-panels">
        <article className="info-panel">
          <h3>Mission complete</h3>
          <p>{lastAutoResponse?.nextAction ? `Next action - ${humanizeAction(lastAutoResponse.nextAction)}` : 'Start the next round to let the system assign missions.'}</p>
          {lastAutoResponse?.autoAssignments?.length ? (
            <ul className="compact-list">
              {lastAutoResponse.autoAssignments.map((entry) => <li key={entry}>{entry}</li>)}
            </ul>
          ) : null}
        </article>
        <article className="info-panel">
          <h3>Dice Outcome</h3>
          <form className="dice-form" onSubmit={handleRollDice}>
            <label>
              Aircraft
              <select value={selectedAircraft} onChange={(event) => setSelectedAircraft(event.target.value)} disabled={!canRollDice}>
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
                  disabled={!canRollDice}
                />
                <span>Random roll</span>
              </label>
              <label className="radio-option">
                <input
                  type="radio"
                  name="diceMode"
                  checked={!useRandomDice}
                  onChange={() => setUseRandomDice(false)}
                  disabled={!canRollDice}
                />
                <span>Choose outcome</span>
              </label>
            </fieldset>
            {!useRandomDice ? (
              <label>
                Dice
                <input type="number" min="1" max="6" value={diceValue} onChange={(event) => setDiceValue(event.target.value)} disabled={!canRollDice} />
              </label>
            ) : null}
            <div className="button-row bottom-actions">
              <button type="button" className="ghost-button" onClick={handleResetGame}>Reset</button>
              <button type="submit" className={nextStep === 'ROLL_DICE' ? 'next-step-button' : ''} disabled={!canRollDice}>Roll dice</button>
            </div>
          </form>
          {lastAutoResponse?.autoLandings?.length ? (
            <ul className="compact-list">
              {lastAutoResponse.autoLandings.map((entry) => <li key={entry}>{entry}</li>)}
            </ul>
          ) : (
            <p>Roll dice to see automatic landing decisions.</p>
          )}
        </article>
      </section>
    </main>
  );
}

function MetricCard({ label, value }) {
  return (
    <article className="metric-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function AircraftStatusCard({ aircraft, additions, compact = false }) {
  const maxStats = {
    fuel: 100,
    weapons: 6,
    hours: 20,
  };
  const positiveAdditions = [
    additions?.fuel ? `Fuel +${additions.fuel}` : null,
    additions?.weapons ? `Weapons +${additions.weapons}` : null,
    additions?.hours ? `Flight hours +${additions.hours}` : null,
  ].filter(Boolean);

  return (
    <div className={`aircraft-status-card${compact ? ' aircraft-status-card-compact' : ''}`}>
      <strong>{aircraft.code}</strong>
      <ul className="aircraft-stats-list">
        <li>Fuel {aircraft.fuel}/{maxStats.fuel}</li>
        <li>Weapons {aircraft.weapons}/{maxStats.weapons}</li>
        <li>Flight hours {aircraft.remainingFlightHours}/{maxStats.hours}</li>
        <li>{aircraft.damage === 'NONE' ? 'Repair none' : `Repair ${humanizeStatus(aircraft.damage)}`}</li>
      </ul>
      {positiveAdditions.length ? <p className="aircraft-added-copy">Added: {positiveAdditions.join(', ')}</p> : null}
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

function scenarioRulesFor(scenarioName) {
  if (scenarioName === 'SCN_STANDARD') {
    return {
      title: 'SCN_STANDARD',
      summary: 'Baseline scenario focused on completing missions with limited aircraft, parking, maintenance capacity, and base resources.',
      points: [
        'Aircraft start with Fuel 100, Weapons 6, and Flight Hours 20.',
        'Mission costs: M1 Recon 20 fuel / 0 weapons / 4 hours, M2 Strike 30 fuel / 2 weapons / 6 hours, M3 Deep Strike 40 fuel / 4 weapons / 8 hours.',
        'Total base capacity in this scenario is 8 parking slots and 3 maintenance slots.',
        'Holding consumes 5 fuel per round until the aircraft can land or is lost.',
        'Fuel deliveries arrive every 2 rounds: A +50, B +40, C +30.',
        'Spare parts arrive every 3 rounds: A +3, B +2, C +0.',
        'Weapons arrive every 4 rounds: A +6, B +4, C +0.',
        'Dice outcomes: 1 no fault, 2-3 minor repair, 4 component damage, 5 major repair, 6 full service required.',
        'Some rounds may have no available actions and can be passed directly to the next round.',
      ],
    };
  }

  return {
    title: scenarioName || 'Scenario',
    summary: 'No scenario description is available for this setup yet.',
    points: [],
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
