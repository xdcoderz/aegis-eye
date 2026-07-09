# aegis-gateway-hub

Spring Boot ingestion service for the AegisEye MVP.

## Responsibility

For the current USP/MVP phase, this service accepts signed evidence records from the edge bridge and stores valid records in PostgreSQL.

## Endpoint

```text
POST /api/evidence
```

The request body is the record emitted by `edge/aegis-eye-bridge/src/prototype.py`.

The gateway:

1. extracts the canonical payload,
2. recomputes its SHA-256 hash,
3. compares it with `payloadHash`,
4. verifies the Ed25519 signature,
5. inserts the record into `evidence_ledger`.

## Run

Start PostgreSQL from the repo root:

```powershell
docker compose -f .\infra\docker-compose.yml up -d
```

Start the gateway:

```powershell
cd .\backend\aegis-gateway-hub
mvn spring-boot:run
```

## Local Configuration

Defaults:

```text
DB_URL=jdbc:postgresql://localhost:55432/aegis_eye
DB_USER=aegis_user
DB_PASSWORD=aegis_dev_password
```

These are local development values only.
