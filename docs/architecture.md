# AegisEye Core Architecture

## V1 Architecture

```text
sample video or frame
  -> edge/aegis-eye-bridge
  -> face detection and redaction
  -> canonical evidence payload
  -> SHA-256 payload hash
  -> Ed25519 signature
  -> backend ingestion API
  -> PostgreSQL evidence_ledger
  -> chain verification
  -> dashboard result
```

## Service Boundaries

### aegis-eye-bridge

Runs near the camera or sample video source.

Responsibilities:

- Capture frames.
- Detect sensitive regions.
- Redact faces.
- Build canonical evidence payloads.
- Compute payload hashes.
- Sign hashes with an edge private key.
- Send signed records to ingestion.

### aegis-gateway-hub

Future backend ingestion service.

Responsibilities:

- Authenticate edge devices.
- Validate payload shape.
- Verify signatures.
- Reject duplicate or replayed sequence numbers.
- Forward accepted records to the ledger.

### aegis-analytics-ledger

Future ledger service.

Responsibilities:

- Persist canonical payloads, hashes, signatures, and chain links.
- Verify continuity for each device and stream.
- Identify the first broken ledger record.

### aegis-portal

Future dashboard.

Responsibilities:

- Show stream health.
- Show recent ledger entries.
- Trigger chain verification.
- Highlight tampered or broken records.

## V1 Non-Goals

- Multi-camera support
- Kubernetes
- Cloud deployment
- Hardware key custody
- User management
- Live production surveillance
- Legal-grade evidence export

These are important later, but they are not part of the first proof loop.
