# AegisEye Core

AegisEye Core is a privacy-preserving, tamper-evident video evidence pipeline.

The v1 goal is deliberately narrow:

```text
sample video/frame -> redact -> canonical evidence payload -> hash -> sign -> store -> verify -> tamper -> detect
```

This repository is a monorepo for the edge computer-vision bridge, backend ingestion/ledger services, frontend portal, shared contracts, and local infrastructure.

## Repository Layout

```text
backend/
  aegis-gateway-hub/        # future ingestion and signature verification API
  aegis-analytics-ledger/   # future ledger persistence and verification service
contracts/
  evidence-payload.schema.json
docs/
  architecture.md
  threat-model.md
  v1-demo-plan.md
edge/
  aegis-eye-bridge/         # Python edge prototype
frontend/
  aegis-portal/             # future React dashboard
infra/
  docker-compose.yml
  postgres/init.sql
```

## Current Phase

The project is in the USP/MVP foundation phase.

Completed in this phase:

- Evidence payload JSON Schema
- PostgreSQL ledger schema
- Local Docker Compose PostgreSQL setup
- Edge prototype that creates, hashes, and signs one canonical payload
- Backend ingestion API scaffold for accepting signed evidence records
- Architecture, threat model, and v1 demo plan docs

## End Goal

The end goal is to build a privacy-preserving, tamper-evident video evidence system:

- sensitive visual data is redacted at the edge,
- evidence payloads are cryptographically hashed and signed,
- records are stored in a verifiable ledger,
- tampering can be detected and localized,
- operators can verify evidence integrity from a dashboard.

For the portfolio version, the target is not a full enterprise surveillance product. The target is a convincing proof that computer vision, cryptography, and backend systems can work together to protect evidence integrity.

## Build Strategy

The project is structured in three stages:

1. USP proof: prove the unique selling point, which is redacted signed evidence plus tamper detection.
2. MVP: make the proof usable end to end with an API, database, verification flow, and basic dashboard.
3. Complete project: add multi-camera support, stronger key custody, production security, exports, and operational polish.

See [docs/project-phases.md](docs/project-phases.md) for the full phase plan.

## Local Requirements

- Docker Desktop
- Python 3.11+ for the edge bridge
- Java 21 and Maven for the backend service
- PowerShell

The local virtual environment currently lives in `edge/aegis-eye-bridge/.venv` and is ignored by git.

## Start PostgreSQL

From the repository root:

```powershell
docker compose -f .\infra\docker-compose.yml up -d
```

PostgreSQL will listen on `localhost:55432`.

Default local credentials:

```text
database: aegis_eye
user: aegis_user
password: aegis_dev_password
```

These are local-only development values. Production secrets must not be stored in source control.

## Run The Edge Prototype

From the repository root:

```powershell
cd .\edge\aegis-eye-bridge
.\.venv\Scripts\python.exe -m src.prototype
```

The prototype generates a synthetic frame, simulates one redaction detection, builds a canonical evidence payload, hashes it, signs it with a demo key, verifies the signature, and prints the resulting record.

To post the generated record to the local gateway:

```powershell
$env:AEGIS_INGEST_URL='http://localhost:8080/api/evidence'
.\.venv\Scripts\python.exe -m src.prototype
Remove-Item Env:\AEGIS_INGEST_URL
```

## Run Edge Tests

From `edge/aegis-eye-bridge`:

```powershell
.\.venv\Scripts\python.exe -m unittest discover -s tests
```

## Run Gateway Hub

From `backend/aegis-gateway-hub`:

```powershell
mvn spring-boot:run
```

The gateway listens on `localhost:8080`.

Useful endpoints:

```text
GET  /health
POST /api/evidence
```

## V1 Success Criterion

The first real milestone is complete when the system can:

1. Generate evidence records from a video source.
2. Persist `payload_hash`, `previous_payload_hash`, canonical payload, and signature.
3. Verify the full chain.
4. Detect and localize a tampered ledger record.

Until that works, dashboard polish and multi-camera support are out of scope.
