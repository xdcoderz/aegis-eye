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

The project is in the technical foundation phase.

Completed in this phase:

- Evidence payload JSON Schema
- PostgreSQL ledger schema
- Local Docker Compose PostgreSQL setup
- Edge prototype that creates, hashes, and signs one canonical payload
- Architecture, threat model, and v1 demo plan docs

## Local Requirements

- Docker Desktop
- Python 3.11+ for the edge bridge
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

## Run Edge Tests

From `edge/aegis-eye-bridge`:

```powershell
.\.venv\Scripts\python.exe -m unittest discover -s tests
```

## V1 Success Criterion

The first real milestone is complete when the system can:

1. Generate evidence records from a video source.
2. Persist `payload_hash`, `previous_payload_hash`, canonical payload, and signature.
3. Verify the full chain.
4. Detect and localize a tampered ledger record.

Until that works, dashboard polish and multi-camera support are out of scope.
