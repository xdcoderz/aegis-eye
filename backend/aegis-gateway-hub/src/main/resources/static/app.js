const state = {
    streams: [],
    records: [],
    selected: null,
};

const elements = {
    health: document.querySelector("#service-health"),
    streamSelect: document.querySelector("#stream-select"),
    refreshButton: document.querySelector("#refresh-button"),
    verifyButton: document.querySelector("#verify-button"),
    tamperButton: document.querySelector("#tamper-button"),
    tamperSequence: document.querySelector("#tamper-sequence"),
    tamperContext: document.querySelector("#tamper-context"),
    result: document.querySelector("#verification-result"),
    recordsBody: document.querySelector("#records-body"),
    emptyState: document.querySelector("#empty-state"),
    metricStreams: document.querySelector("#metric-streams"),
    metricRecords: document.querySelector("#metric-records"),
    metricIntegrity: document.querySelector("#metric-integrity"),
    metricSequence: document.querySelector("#metric-sequence"),
};

async function api(path, options = {}) {
    const response = await fetch(path, {
        ...options,
        headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || body.message || `${response.status} ${response.statusText}`);
    }
    return response.json();
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function shortHash(value) {
    return value ? `${value.slice(0, 10)}...${value.slice(-6)}` : "GENESIS";
}

function formatTime(value) {
    return value ? new Intl.DateTimeFormat(undefined, {
        dateStyle: "medium",
        timeStyle: "medium",
    }).format(new Date(value)) : "-";
}

function setBusy(isBusy) {
    elements.refreshButton.disabled = isBusy;
    elements.verifyButton.disabled = isBusy || !state.selected;
    elements.tamperButton.disabled = isBusy || !state.selected?.streamId.startsWith("demo-");
}

function showResult(status, title, detail) {
    elements.result.className = `result-panel ${status}`;
    elements.result.innerHTML = `<strong>${escapeHtml(title)}</strong><span>${escapeHtml(detail)}</span>`;
}

function renderMetrics() {
    const totalRecords = state.streams.reduce((sum, stream) => sum + stream.recordCount, 0);
    elements.metricStreams.textContent = state.streams.length;
    elements.metricRecords.textContent = totalRecords;
    elements.metricIntegrity.textContent = state.selected
        ? state.selected.integrityStatus.toUpperCase()
        : "No data";
    elements.metricSequence.textContent = state.selected?.lastSequenceNumber ?? "-";
}

function renderRecords() {
    elements.recordsBody.innerHTML = state.records.map((record) => `
        <tr>
            <td><strong>#${record.sequenceNumber}</strong></td>
            <td>${escapeHtml(formatTime(record.capturedAt))}</td>
            <td class="hash" title="${escapeHtml(record.payloadHash)}">${escapeHtml(shortHash(record.payloadHash))}</td>
            <td class="hash" title="${escapeHtml(record.previousPayloadHash || "Genesis record")}">${escapeHtml(shortHash(record.previousPayloadHash))}</td>
            <td>${record.detectionCount}</td>
            <td>${escapeHtml(record.redactionModel)}</td>
            <td><span class="status-pill ${escapeHtml(record.verificationStatus)}">${escapeHtml(record.verificationStatus)}</span></td>
        </tr>
    `).join("");
    elements.emptyState.classList.toggle("visible", state.records.length === 0);
}

async function loadRecords() {
    if (!state.selected) {
        state.records = [];
        renderRecords();
        return;
    }
    const query = new URLSearchParams({
        deviceId: state.selected.deviceId,
        streamId: state.selected.streamId,
    });
    state.records = await api(`/api/ledger/records?${query}`);
    elements.tamperSequence.max = state.selected.lastSequenceNumber;
    elements.tamperSequence.value = state.selected.lastSequenceNumber;
    const demoEnabled = state.selected.streamId.startsWith("demo-");
    elements.tamperContext.textContent = demoEnabled
        ? `${state.selected.streamId} is enabled for controlled mutation.`
        : "Intentional mutation is limited to demo-prefixed streams.";
    elements.tamperButton.disabled = !demoEnabled;
    renderRecords();
}

async function refresh() {
    setBusy(true);
    const priorKey = state.selected ? `${state.selected.deviceId}/${state.selected.streamId}` : null;
    try {
        state.streams = await api("/api/ledger/streams");
        elements.streamSelect.innerHTML = state.streams.length
            ? state.streams.map((stream, index) => {
                const key = `${stream.deviceId}/${stream.streamId}`;
                return `<option value="${index}" ${key === priorKey ? "selected" : ""}>${escapeHtml(stream.streamId)} | ${escapeHtml(stream.deviceId)}</option>`;
            }).join("")
            : '<option value="">No streams</option>';
        const selectedIndex = Number(elements.streamSelect.value || 0);
        state.selected = state.streams[selectedIndex] || null;
        await loadRecords();
        renderMetrics();
        if (state.selected) {
            showResult(
                state.selected.integrityStatus,
                `${state.selected.integrityStatus.toUpperCase()} ledger state`,
                `${state.selected.recordCount} records available for verification.`
            );
        }
    } catch (error) {
        showResult("broken", "Could not load ledger", error.message);
    } finally {
        setBusy(false);
    }
}

async function verifySelected() {
    if (!state.selected) return;
    setBusy(true);
    showResult("pending", "Verification running", "Recomputing hashes, signatures, metadata, sequence, and chain links.");
    try {
        const result = await api("/api/ledger/verify", {
            method: "POST",
            body: JSON.stringify({
                deviceId: state.selected.deviceId,
                streamId: state.selected.streamId,
            }),
        });
        await refresh();
        showResult(
            result.status,
            result.status === "valid" ? "Chain verified" : `Break found at sequence ${result.firstBrokenSequenceNumber}`,
            `${result.reason} Checked ${result.checkedRecords} of ${result.totalRecords} records.`
        );
    } catch (error) {
        showResult("broken", "Verification failed", error.message);
    } finally {
        setBusy(false);
    }
}

async function tamperSelected() {
    if (!state.selected) return;
    setBusy(true);
    try {
        const sequenceNumber = Number(elements.tamperSequence.value);
        await api("/api/demo/tamper", {
            method: "POST",
            body: JSON.stringify({
                deviceId: state.selected.deviceId,
                streamId: state.selected.streamId,
                sequenceNumber,
            }),
        });
        showResult("pending", `Sequence ${sequenceNumber} mutated`, "The signed hash was left unchanged. Run verification to locate the break.");
        await loadRecords();
    } catch (error) {
        showResult("broken", "Tamper simulation failed", error.message);
    } finally {
        setBusy(false);
    }
}

async function checkHealth() {
    try {
        await api("/health");
        elements.health.className = "health-badge online";
        elements.health.textContent = "Gateway online";
    } catch {
        elements.health.className = "health-badge offline";
        elements.health.textContent = "Gateway offline";
    }
}

elements.streamSelect.addEventListener("change", async () => {
    state.selected = state.streams[Number(elements.streamSelect.value)] || null;
    await loadRecords();
    renderMetrics();
});
elements.refreshButton.addEventListener("click", refresh);
elements.verifyButton.addEventListener("click", verifySelected);
elements.tamperButton.addEventListener("click", tamperSelected);

checkHealth();
refresh();
