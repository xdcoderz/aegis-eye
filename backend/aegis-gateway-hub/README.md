# aegis-gateway-hub

Spring Boot service for AegisEye evidence ingestion, device-key pairing, PostgreSQL ledger persistence, stored-chain verification, and the browser console.

## Run

Start PostgreSQL from the repository root:

```powershell
docker compose -f .\infra\docker-compose.yml up -d
```

Start this service:

```powershell
cd .\backend\aegis-gateway-hub
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

## API Surface

```text
GET  /health
POST /api/evidence
GET  /api/ledger/streams
GET  /api/ledger/records
POST /api/ledger/verify
POST /api/demo/tamper
```

The tamper endpoint is restricted to `demo-*` streams and exists only for the local hackathon demonstration.

## Configuration

```text
DB_URL=jdbc:postgresql://localhost:55432/aegis_eye
DB_USER=aegis_user
DB_PASSWORD=aegis_dev_password
AEGIS_DEMO_TAMPER_ENABLED=true
```

Set `AEGIS_DEMO_TAMPER_ENABLED=false` outside a controlled demonstration.

## Verification Invariants

The stored-chain verifier checks:

- contiguous sequence numbers beginning at 1,
- row identity and sequence against the canonical payload,
- recomputed canonical payload hash,
- previous-hash value in both row and payload,
- link to the preceding record,
- supported signature algorithm,
- Ed25519 signature against the paired device public key.

## Tests

```powershell
mvn test
```

See `docs/api.md` and `docs/architecture.md` for the full contract and design.
