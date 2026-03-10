const gameIdInput = document.getElementById("game-id");
const gameStateOutput = document.getElementById("game-state");
const landingOutput = document.getElementById("landing-output");
const detailOutput = document.getElementById("detail-output");
const eventLog = document.getElementById("event-log");
const rulesSummary = document.getElementById("rules-summary");

function appendLog(message, data) {
    const row = document.createElement("div");
    row.className = "log-entry";
    const timestamp = new Date().toLocaleTimeString("sv-SE");
    row.textContent = `[${timestamp}] ${message}`;
    if (data) {
        const pre = document.createElement("pre");
        pre.className = "json-inline";
        pre.textContent = JSON.stringify(data, null, 2);
        row.appendChild(pre);
    }
    eventLog.prepend(row);
}

async function api(path, options = {}) {
    const response = await fetch(path, {
        headers: {
            "Content-Type": "application/json"
        },
        ...options
    });

    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
        throw new Error(data?.message || text || `Request failed with ${response.status}`);
    }
    return data;
}

function requireGameId() {
    const gameId = gameIdInput.value.trim();
    if (!gameId) {
        throw new Error("Ange gameId först.");
    }
    return gameId;
}

function renderJson(element, data) {
    element.textContent = JSON.stringify(data, null, 2);
}

async function refreshGameState() {
    const gameId = requireGameId();
    const data = await api(`/api/games/${encodeURIComponent(gameId)}`);
    renderJson(gameStateOutput, data);
    appendLog("Game state uppdaterad", data);
}

function formJson(form) {
    return Object.fromEntries(new FormData(form).entries());
}

document.getElementById("create-game-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const payload = formJson(event.target);
    const data = await api("/api/games", {
        method: "POST",
        body: JSON.stringify(payload)
    });
    const inferredGameId = data.gameId || data.id || data.game?.id || "";
    if (inferredGameId) {
        gameIdInput.value = inferredGameId;
    }
    renderJson(gameStateOutput, data);
    appendLog("Nytt spel skapat", data);
});

document.querySelectorAll("[data-action]").forEach((button) => {
    button.addEventListener("click", async () => {
        try {
            const gameId = requireGameId();
            const action = button.dataset.action;
            let data;
            if (action === "refresh-state") {
                await refreshGameState();
                return;
            }
            if (action === "start-round") {
                data = await api(`/api/games/${encodeURIComponent(gameId)}/rounds/start`, {method: "POST"});
            }
            if (action === "resolve-missions") {
                data = await api(`/api/games/${encodeURIComponent(gameId)}/missions/resolve`, {method: "POST"});
            }
            if (action === "complete-round") {
                data = await api(`/api/games/${encodeURIComponent(gameId)}/rounds/complete`, {method: "POST"});
            }
            renderJson(gameStateOutput, data);
            appendLog(`Åtgärd ${action} utförd`, data);
        }
        catch (error) {
            appendLog(error.message);
        }
    });
});

document.getElementById("assign-mission-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        const gameId = requireGameId();
        const data = await api(`/api/games/${encodeURIComponent(gameId)}/missions/assign`, {
            method: "POST",
            body: JSON.stringify(formJson(event.target))
        });
        renderJson(gameStateOutput, data);
        appendLog("Uppdrag tilldelat", data);
    }
    catch (error) {
        appendLog(error.message);
    }
});

document.getElementById("dice-roll-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        const gameId = requireGameId();
        const payload = formJson(event.target);
        payload.diceValue = Number(payload.diceValue);
        const data = await api(`/api/games/${encodeURIComponent(gameId)}/dice-rolls`, {
            method: "POST",
            body: JSON.stringify(payload)
        });
        renderJson(gameStateOutput, data);
        appendLog("Tärningsslag registrerat", data);
    }
    catch (error) {
        appendLog(error.message);
    }
});

document.getElementById("landing-bases-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        const gameId = requireGameId();
        const aircraftCode = formJson(event.target).aircraftCode;
        const data = await api(`/api/games/${encodeURIComponent(gameId)}/landing-bases?aircraftCode=${encodeURIComponent(aircraftCode)}`);
        renderJson(landingOutput, data);
        appendLog("Landningsbaser hämtade", data);
    }
    catch (error) {
        appendLog(error.message);
    }
});

document.getElementById("land-aircraft-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        const gameId = requireGameId();
        const data = await api(`/api/games/${encodeURIComponent(gameId)}/landings`, {
            method: "POST",
            body: JSON.stringify(formJson(event.target))
        });
        renderJson(landingOutput, data);
        appendLog("Flygplan landat", data);
    }
    catch (error) {
        appendLog(error.message);
    }
});

document.getElementById("holding-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        const gameId = requireGameId();
        const aircraftCode = formJson(event.target).aircraftCode;
        const data = await api(`/api/games/${encodeURIComponent(gameId)}/holding?aircraftCode=${encodeURIComponent(aircraftCode)}`, {
            method: "POST"
        });
        renderJson(landingOutput, data);
        appendLog("Flygplan skickat till holding", data);
    }
    catch (error) {
        appendLog(error.message);
    }
});

document.getElementById("aircraft-state-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        const gameId = requireGameId();
        const aircraftCode = formJson(event.target).aircraftCode;
        const data = await api(`/api/games/${encodeURIComponent(gameId)}/aircraft/${encodeURIComponent(aircraftCode)}`);
        renderJson(detailOutput, data);
        appendLog("Aircraft state hämtad", data);
    }
    catch (error) {
        appendLog(error.message);
    }
});

document.getElementById("base-state-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        const gameId = requireGameId();
        const baseCode = formJson(event.target).baseCode;
        const data = await api(`/api/games/${encodeURIComponent(gameId)}/bases/${encodeURIComponent(baseCode)}`);
        renderJson(detailOutput, data);
        appendLog("Base state hämtad", data);
    }
    catch (error) {
        appendLog(error.message);
    }
});

async function loadRules() {
    try {
        const rules = await api("/api/reference/rules");
        rulesSummary.innerHTML = `
            <p><strong>Mål:</strong> ${rules.objectives.join(" ")}</p>
            <p><strong>Missioner:</strong> ${rules.missions.map((m) => `${m.code} ${m.name} (${m.fuelCost} fuel, ${m.weaponCost} weapons, ${m.flightHours} h)`).join(" | ")}</p>
            <p><strong>Baser:</strong> ${rules.bases.map((b) => `${b.code}: parkering ${b.parkingSlots}, maintenance ${b.maintenanceSlots}, tjänster ${b.capabilities.join(", ")}`).join(" | ")}</p>
            <p><strong>Leveranser:</strong> bränsle var ${rules.resourceRules.fuelDeliveries.frequencyRounds}:a, reservdelar var ${rules.resourceRules.sparePartDeliveries.frequencyRounds}:e, vapen var ${rules.resourceRules.weaponDeliveries.frequencyRounds}:e runda.</p>
            <p><strong>Holding:</strong> ${rules.resourceRules.holdingFuelCostPerRound} fuel per runda.</p>
            <p><strong>Rundflöde:</strong> ${rules.roundPhases.join(" ")}</p>
        `;
    }
    catch (error) {
        rulesSummary.textContent = error.message;
    }
}

loadRules();
