# AegisEye Prototype API

Base URL: `http://localhost:8080`

## Health

### `GET /health`

Returns gateway availability.

```json
{"status":"ok"}
```

## Evidence Ingestion

### `POST /api/evidence`

Accepts a signed evidence envelope.

Required envelope fields:

- `payloadHash`: lowercase SHA-256 hex of canonical payload bytes
- `previousPayloadHash`: null for sequence 1, otherwise prior payload hash
- `signature`: Base64 Ed25519 signature over the ASCII payload hash
- `signatureAlgorithm`: `ed25519`
- `publicKeyId`: registered key identifier
- `publicKeyHex`: raw 32-byte Ed25519 public key in lowercase hex
- `canonicalPayload`: object matching `contracts/evidence-payload.schema.json`

Success: `201 Created`

```json
{
  "ledgerId": "3e196a73-1874-4bb7-b18a-fb246558c88c",
  "deviceId": "edge-demo-001",
  "streamId": "demo-synthetic-20260714-110000",
  "sequenceNumber": 1,
  "payloadHash": "64 lowercase hex characters",
  "verificationStatus": "valid"
}
```

Failures:

- `400`: malformed payload, invalid hash/signature, wrong key, or broken chain position
- `409`: duplicate stream sequence or duplicate payload hash

## Ledger Streams

### `GET /api/ledger/streams`

Returns stream summaries ordered by latest received record.

## Ledger Records

### `GET /api/ledger/records?deviceId=...&streamId=...`

Returns sequence metadata, hashes, detection count, model, and latest verification result.

## Chain Verification

### `POST /api/ledger/verify`

Request:

```json
{
  "deviceId": "edge-demo-001",
  "streamId": "demo-synthetic-20260714-110000"
}
```

Valid response:

```json
{
  "deviceId": "edge-demo-001",
  "streamId": "demo-synthetic-20260714-110000",
  "status": "valid",
  "checkedRecords": 5,
  "totalRecords": 5,
  "firstBrokenSequenceNumber": null,
  "reason": "Every stored payload, signature, sequence, and chain link is valid",
  "verifiedAt": "2026-07-14T06:00:00Z"
}
```

Broken response uses `status: "broken"`, includes `firstBrokenSequenceNumber`, and explains the failed invariant.

## Controlled Tamper Simulation

### `POST /api/demo/tamper`

Available only when `AEGIS_DEMO_TAMPER_ENABLED=true` and only for stream IDs beginning with `demo-`.

```json
{
  "deviceId": "edge-demo-001",
  "streamId": "demo-synthetic-20260714-110000",
  "sequenceNumber": 3
}
```

This intentionally changes the stored redaction-model version without updating its signed payload hash. It exists solely to demonstrate post-storage detection. Set `AEGIS_DEMO_TAMPER_ENABLED=false` outside a local demonstration.
