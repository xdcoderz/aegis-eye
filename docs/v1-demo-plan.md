# AegisEye Hackathon Demo Runbook

## Demo Goal

In under three minutes, prove that AegisEye redacts sensitive visual data at the edge, binds evidence to a device, and detects a database mutation at the exact affected sequence.

## Pre-Demo Check

From the repository root:

```powershell
docker compose -f .\infra\docker-compose.yml up -d
docker compose -f .\infra\docker-compose.yml ps
```

Run tests:

```powershell
cd .\edge\aegis-eye-bridge
.\.venv\Scripts\python.exe -m unittest discover -s tests

cd ..\..\backend\aegis-gateway-hub
mvn test
```

Start the backend:

```powershell
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

## Reliable Three-Minute Demo

### 1. Frame the problem

Suggested line:

> Computer vision can redact faces, but a reviewer still needs to know whether the evidence was changed later. AegisEye joins edge privacy with cryptographic chain of custody.

### 2. Create evidence

In the edge directory:

```powershell
.\.venv\Scripts\python.exe -m src.demo_seed
```

This creates five linked records with a persistent device signature and submits them to the gateway.

### 3. Verify the intact chain

Refresh the console, select the newest `demo-synthetic-*` stream, and select `Verify chain`.

Expected result:

```text
VALID
Every stored payload, signature, sequence, and chain link is valid.
```

### 4. Simulate post-storage tampering

Choose a middle sequence such as `3`, select `Tamper record`, then select `Verify chain` again.

Expected result:

```text
BROKEN at sequence 3
Canonical payload hash mismatch.
```

### 5. Close with value

Suggested line:

> The original private key never entered the backend. AegisEye independently found the first modified record by re-hashing stored evidence and checking its device signature and chain link.

## Real CV Demonstration

Use this when the webcam is reliable:

```powershell
$stream = "demo-webcam-" + (Get-Date -Format "yyyyMMdd-HHmmss")
.\.venv\Scripts\python.exe -m src.video_pipeline --source 0 --stream-id $stream --display
```

Stand facing the camera with reasonable frontal lighting. The preview and output MP4 show the redaction. Evidence is submitted every 30 frames by default.

For a prerecorded file:

```powershell
$stream = "demo-video-" + (Get-Date -Format "yyyyMMdd-HHmmss")
.\.venv\Scripts\python.exe -m src.video_pipeline --source "D:\path\sample.mp4" --stream-id $stream
```

## Demo Recovery

- Camera unavailable: use `src.demo_seed`.
- Port 8080 occupied: set `SERVER_PORT` and pass the matching `--ingest-url`; note that the console URL changes too.
- Duplicate stream sequence: use a new timestamped stream ID.
- Device key mismatch after replacing `keys/demo-device-ed25519.pem`: reset or re-enroll the local demo device before presenting. Do not delete the key between normal runs.
- No streams in the console: confirm the gateway accepted records and refresh.

## Acceptance Criteria

- At least three sequential records are accepted.
- The intact stream verifies as valid.
- A controlled mutation changes only the stored canonical payload.
- Re-verification reports `broken` and the affected sequence.
- The real CV path produces a redacted video and linked manifest.
- All automated tests pass.
