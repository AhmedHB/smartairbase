"use client";

import { useEffect, useState } from "react";

import type { GameState } from "@/lib/game-types";

const SESSION_STORAGE_KEY = "fleet-command-web-session";

const panelClass =
  "rounded-[32px] border border-line bg-panel p-10 shadow-panel backdrop-blur-[18px] sm:p-12";
const cardClass =
  "rounded-[24px] border border-[rgba(47,59,50,0.12)] bg-[rgba(255,251,244,0.9)] p-6 sm:p-7";
const eyebrowClass =
  "font-mono text-[0.76rem] uppercase tracking-[0.18em] text-muted";
const titleClass = "text-[clamp(1.6rem,3vw,2.6rem)] font-semibold";
const mutedClass = "text-sm leading-[1.55] text-muted sm:text-[0.95rem]";
const chipClass =
  "rounded-full border border-[rgba(47,59,50,0.08)] bg-[rgba(243,238,226,0.95)] px-3 py-2 text-[0.82rem]";
const monoClass = "font-mono";
const primaryButtonClass =
  "min-h-14 rounded-[20px] bg-[linear-gradient(135deg,var(--accent),var(--accent-deep))] px-6 text-base font-semibold text-[#f6f2e7] shadow-[0_16px_30px_rgba(45,83,53,0.22)] transition duration-150 ease-out hover:-translate-y-px disabled:cursor-not-allowed disabled:opacity-60 disabled:translate-y-0";

function cx(...classes: Array<string | false | null | undefined>) {
  return classes.filter(Boolean).join(" ");
}

function statusTone(status: string) {
  if (status === "AVAILABLE") return "available";
  if (status === "IN_TRANSIT") return "transit";
  if (status === "ON_MISSION") return "mission";
  return "maintenance";
}

function statusBadgeClass(status: string) {
  const tone = statusTone(status);
  const toneClass =
    tone === "available"
      ? "bg-[rgba(49,126,70,0.12)] text-[#205a32]"
      : tone === "transit"
        ? "bg-[rgba(180,121,45,0.12)] text-[#8a5c1f]"
        : tone === "mission"
          ? "bg-[rgba(38,100,133,0.12)] text-[#225f80]"
          : "bg-[rgba(149,58,42,0.12)] text-[#8f3f31]";

  return cx(
    "rounded-full px-3 py-2 font-mono text-[0.74rem] uppercase tracking-[0.08em]",
    toneClass,
  );
}

function formatSigned(value: number | null) {
  if (value === null) {
    return "No turn yet";
  }
  return `${value >= 0 ? "+" : ""}${value.toFixed(2)}`;
}

function formatHours(value: number | null | undefined) {
  if (value === null || value === undefined) {
    return "0.0h";
  }
  return `${value.toFixed(1)}h`;
}

function formatRatio(current: number, maximum: number) {
  if (maximum <= 0) {
    return "0%";
  }
  return `${Math.round((current / maximum) * 100)}%`;
}

function humanizeStatus(status: string) {
  return status.toLowerCase().replaceAll("_", " ");
}

function plannedActionText(aircraft: GameState["aircraft"][number]) {
  if (aircraft.form.available) {
    return aircraft.advisor.summary;
  }

  if (aircraft.statusTiming.readyInHours !== null) {
    return `continue ${humanizeStatus(aircraft.status)} · ready in ${formatHours(
      aircraft.statusTiming.readyInHours,
    )}`;
  }

  return `continue ${humanizeStatus(aircraft.status)}`;
}

export default function Home() {
  const [game, setGame] = useState<GameState | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadingLabel, setLoadingLabel] = useState("Loading");
  const [error, setError] = useState<string | null>(null);
  const [restoring, setRestoring] = useState(true);

  useEffect(() => {
    const savedSessionId = window.localStorage.getItem(SESSION_STORAGE_KEY);
    if (!savedSessionId) {
      setRestoring(false);
      return;
    }

    void (async () => {
      setLoading(true);
      setLoadingLabel("Restoring");
      setError(null);

      try {
        const response = await fetch(`/api/game/${savedSessionId}`, {
          cache: "no-store",
        });
        const payload = await response.json();
        if (!response.ok) {
          throw new Error(payload.error ?? "Could not load game.");
        }
        applyGameState(payload as GameState);
      } catch (nextError) {
        window.localStorage.removeItem(SESSION_STORAGE_KEY);
        setGame(null);
        setError(
          nextError instanceof Error
            ? nextError.message
            : "Could not restore the previous session.",
        );
      } finally {
        setLoading(false);
        setRestoring(false);
      }
    })();
  }, []);

  function applyGameState(nextGame: GameState) {
    setGame(nextGame);
    window.localStorage.setItem(SESSION_STORAGE_KEY, nextGame.sessionId);
  }

  async function openRun() {
    setLoading(true);
    setLoadingLabel("Opening");
    setError(null);

    try {
      const response = await fetch("/api/game", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({}),
      });
      const payload = await response.json();
      if (!response.ok) {
        throw new Error(payload.error ?? "Could not start game.");
      }
      applyGameState(payload as GameState);
    } catch (nextError) {
      setError(
        nextError instanceof Error
          ? nextError.message
          : "Could not start the fleet command session.",
      );
    } finally {
      setLoading(false);
      setRestoring(false);
    }
  }

  async function nextRound() {
    if (!game) {
      return;
    }

    setLoading(true);
    setLoadingLabel("Advancing");
    setError(null);

    try {
      const response = await fetch(`/api/game/${game.sessionId}/turn`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({}),
      });
      const payload = await response.json();
      if (!response.ok) {
        throw new Error(payload.error ?? "Could not play turn.");
      }
      applyGameState(payload as GameState);
    } catch (nextError) {
      setError(
        nextError instanceof Error
          ? nextError.message
          : "Could not submit the turn.",
      );
    } finally {
      setLoading(false);
    }
  }

  const summary = game
    ? (() => {
        const readyNowCount = game.readiness.filter((item) => item.readyNow).length;
        const onMissionCount = game.aircraft.filter(
          (aircraft) => aircraft.status === "ON_MISSION",
        ).length;
        const transitCount = game.aircraft.filter(
          (aircraft) => aircraft.status === "IN_TRANSIT",
        ).length;
        const maintenanceCount = game.aircraft.filter(
          (aircraft) => aircraft.status === "UNDER_MAINTENANCE",
        ).length;
        const totalFuel = game.bases.reduce((sum, base) => sum + base.fuel, 0);
        const totalFuelMax = game.bases.reduce((sum, base) => sum + base.fuelMax, 0);
        const totalSpares = game.bases.reduce((sum, base) => sum + base.spares, 0);
        const totalSparesMax = game.bases.reduce((sum, base) => sum + base.sparesMax, 0);
        const nextReady = [...game.readiness]
          .filter((item) => !item.readyNow && item.readyInHours !== null)
          .sort(
            (left, right) =>
              (left.readyInHours ?? Number.POSITIVE_INFINITY) -
              (right.readyInHours ?? Number.POSITIVE_INFINITY),
          )[0];

        return {
          readyNowCount,
          onMissionCount,
          transitCount,
          maintenanceCount,
          fuelRatio: formatRatio(totalFuel, totalFuelMax),
          sparesRatio: formatRatio(totalSpares, totalSparesMax),
          nextReady,
        };
      })()
    : null;

  return (
    <div className="min-h-screen px-6 py-8 sm:px-10 sm:py-10 lg:px-14 xl:px-16">
      <main className="mx-auto grid w-full max-w-[1560px] gap-10">
        <section
          className={cx(
            panelClass,
            "flex flex-col gap-8 xl:flex-row xl:items-center xl:justify-between",
          )}
        >
          <div className="space-y-3">
            <p className={eyebrowClass}>Autonom Air Base</p>
            <h1 className="text-[clamp(2.2rem,5vw,4.4rem)] leading-[0.95] font-semibold">
              Fleet Pulse
            </h1>
            <p className="max-w-[34rem] text-base leading-[1.6] text-muted">
              RL-trained airbase control. Tap once for the next round.
            </p>
          </div>

          <button
            className={primaryButtonClass}
            disabled={loading || Boolean(game?.meta.finished)}
            onClick={game ? nextRound : openRun}
            type="button"
          >
            {game
              ? game.meta.finished
                ? "Run Complete"
                : "Next Round"
              : "Open Run"}
          </button>
        </section>

        {loading || restoring ? (
          <section className={cx(panelClass, "py-5")}>
            <p className="text-base">{loadingLabel}...</p>
          </section>
        ) : null}

        {error ? (
          <section
            className={cx(
              panelClass,
              "border-[rgba(150,65,47,0.25)] bg-[rgba(255,240,233,0.85)] py-5",
            )}
          >
            <p className="text-base">{error}</p>
          </section>
        ) : null}

        {!game ? (
          <section className={cx(panelClass, "py-14 text-center")}>
            <p className="text-xl font-semibold">Ready to open a run.</p>
          </section>
        ) : (
          <>
            {game.meta.resultLabel ? (
              <section
                className={cx(
                  panelClass,
                  "border-[rgba(47,93,59,0.25)] bg-[rgba(238,247,239,0.92)] py-5",
                )}
              >
                <p className="text-base">
                  <strong>{game.meta.resultLabel}.</strong> Final score{" "}
                  {game.meta.score.toFixed(1)}.
                </p>
              </section>
            ) : null}

            <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
              <article className={cardClass}>
                <p className={eyebrowClass}>Turn</p>
                <p className="mt-5 text-[clamp(1.7rem,2vw,2.4rem)] font-bold">
                  {game.meta.turn}
                </p>
              </article>
              <article className={cardClass}>
                <p className={eyebrowClass}>Time</p>
                <p className="mt-5 text-[clamp(1.7rem,2vw,2.4rem)] font-bold">
                  {game.meta.timeHours.toFixed(0)}h
                </p>
              </article>
              <article className={cardClass}>
                <p className={eyebrowClass}>Score</p>
                <p className="mt-5 text-[clamp(1.7rem,2vw,2.4rem)] font-bold">
                  {game.meta.score.toFixed(1)}
                </p>
              </article>
              <article className={cardClass}>
                <p className={eyebrowClass}>Missions</p>
                <p className="mt-5 text-[clamp(1.7rem,2vw,2.4rem)] font-bold">
                  {game.meta.missionsCompleted}/{game.meta.totalMissions}
                </p>
              </article>
              <article className={cardClass}>
                <p className={eyebrowClass}>Last Turn</p>
                <p
                  className={cx(
                    "mt-5 text-[clamp(1.5rem,2vw,2.2rem)] font-bold",
                    (game.meta.lastTurnReward ?? 0) >= 0
                      ? "text-accent-deep"
                      : "text-danger",
                  )}
                >
                  {formatSigned(game.meta.lastTurnReward)}
                </p>
              </article>
            </section>

            <section className={cx(panelClass, "grid gap-6")}>
              <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                <div>
                  <p className={eyebrowClass}>This Round</p>
                  <h2 className={titleClass}>Planned actions</h2>
                </div>
                <p className={mutedClass}>What each aircraft will do next</p>
              </div>

              <div className="grid gap-4 xl:grid-cols-2">
                {game.aircraft.map((aircraft) => (
                  <article
                    className="rounded-[22px] border border-[rgba(47,59,50,0.12)] bg-[rgba(255,251,244,0.9)] px-5 py-5"
                    key={aircraft.id}
                  >
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                      <div className="flex flex-wrap items-center gap-2.5">
                        <strong>{aircraft.name}</strong>
                        <span className={monoClass}>{aircraft.baseName}</span>
                        <span className={statusBadgeClass(aircraft.status)}>
                          {humanizeStatus(aircraft.status)}
                        </span>
                      </div>
                      <p className={mutedClass}>
                        Fuel {aircraft.fuelLevel.toFixed(0)}
                      </p>
                    </div>
                    <p className="mt-4 text-[1.02rem] leading-[1.55] text-ink">
                      {plannedActionText(aircraft)}
                    </p>
                  </article>
                ))}
              </div>
            </section>

            <section className={cx(panelClass, "grid gap-6")}>
              <div className="grid gap-4 xl:grid-cols-3">
                <article className={cardClass}>
                  <p className={eyebrowClass}>Fleet</p>
                  <p className="mt-4 text-2xl font-semibold">
                    {summary?.readyNowCount ?? 0}/{game.aircraft.length} ready
                  </p>
                  <p className={cx(mutedClass, "mt-3")}>
                    {summary?.onMissionCount ?? 0} mission ·{" "}
                    {summary?.transitCount ?? 0} transit ·{" "}
                    {summary?.maintenanceCount ?? 0} maintenance
                  </p>
                  <div className="mt-4 flex flex-wrap gap-2">
                    {game.readiness
                      .filter((item) => item.readyNow)
                      .slice(0, 6)
                      .map((item) => (
                        <span className={chipClass} key={item.aircraftId}>
                          {item.aircraftName}
                        </span>
                      ))}
                  </div>
                </article>

                <article className={cardClass}>
                  <p className={eyebrowClass}>Bases</p>
                  <p className="mt-4 text-2xl font-semibold">
                    Fuel {summary?.fuelRatio ?? "0%"}
                  </p>
                  <p className={cx(mutedClass, "mt-3")}>
                    Spares {summary?.sparesRatio ?? "0%"}
                  </p>
                  <div className="mt-4 flex flex-wrap gap-2">
                    {game.bases.map((base) => (
                      <span className={chipClass} key={base.id}>
                        {base.name} · {base.parkingUsed}/{base.parkingSlots}
                      </span>
                    ))}
                  </div>
                </article>

                <article className={cardClass}>
                  <p className={eyebrowClass}>Missions</p>
                  <p className="mt-4 text-2xl font-semibold">
                    {game.missions.length} open
                  </p>
                  <p className={cx(mutedClass, "mt-3")}>
                    {summary?.nextReady
                      ? `${summary.nextReady.aircraftName} ready in ${formatHours(summary.nextReady.readyInHours)}`
                      : "Aircraft ready now"}
                  </p>
                  <div className="mt-4 flex flex-wrap gap-2">
                    {game.missions.length > 0 ? (
                      game.missions.map((mission) => (
                        <span className={chipClass} key={mission.slot}>
                          {mission.name}
                        </span>
                      ))
                    ) : (
                      <span className={chipClass}>No open missions</span>
                    )}
                  </div>
                </article>
              </div>
            </section>

            <section className={cx(panelClass, "grid gap-5")}>
              <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                <div>
                  <p className={eyebrowClass}>Aircraft</p>
                  <h2 className={titleClass}>Readiness</h2>
                </div>
                <p className={mutedClass}>
                  Session <span className={monoClass}>{game.sessionId}</span>
                </p>
              </div>

              <div className="grid gap-3 xl:grid-cols-2">
                {game.readiness.map((item) => (
                  <article
                    className="rounded-[22px] border border-[rgba(47,59,50,0.12)] bg-[rgba(255,251,244,0.9)] px-5 py-4"
                    key={item.aircraftId}
                  >
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                      <div className="flex flex-wrap items-center gap-2.5">
                        <strong>{item.aircraftName}</strong>
                        <span className={monoClass}>{item.baseName}</span>
                        <span className={statusBadgeClass(item.status)}>
                          {humanizeStatus(item.status)}
                        </span>
                      </div>
                      <p className={mutedClass}>
                        {item.readyNow
                          ? `Ready for ${formatHours(item.readyForHours)}`
                          : `${humanizeStatus(item.status)} · ready in ${formatHours(item.readyInHours)}`}
                      </p>
                    </div>
                  </article>
                ))}
              </div>
            </section>
          </>
        )}
      </main>
    </div>
  );
}
