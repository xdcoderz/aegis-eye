# AegisEye USP and MVP Demo Plan

## End Goal

Build a system that can take privacy-redacted visual evidence from an edge device, prove it was signed by that device, store it in a ledger, and detect later tampering.

## USP Objective

Show the unique selling point first:

```text
redacted evidence -> canonical payload -> payload hash -> signature -> ledger -> tamper detection
```

This must work before adding product polish.

## MVP Objective

Turn the USP into a usable local demo:

- one edge producer,
- one ingestion API,
- one PostgreSQL ledger,
- one chain verification flow,
- one minimal dashboard.

## Demo Story

1. Start PostgreSQL.
2. Run the edge bridge prototype.
3. Produce a signed evidence payload.
4. Store the record in the ledger.
5. Verify the chain.
6. Manually tamper with one row.
7. Verify again.
8. Show the broken record.

## Phase 1: Foundation

Status: complete

- Define evidence payload schema.
- Define ledger database table.
- Create local PostgreSQL Compose setup.
- Create edge prototype for canonical payload, hash, and signature.

## Phase 2: Ingestion MVP

Status: complete

- Create `POST /api/evidence`.
- Recompute canonical payload hash.
- Verify Ed25519 signature.
- Persist accepted records to PostgreSQL.
- Reject tampered or invalid records.

## Phase 3: Edge Computer Vision

- Read a sample video.
- Detect faces with a simple OpenCV detector or ONNX model.
- Redact detected faces.
- Hash redacted frame output.
- Emit one payload per frame batch.

## Phase 4: Chain Verification

Status: next

- Create `POST /api/ledger/verify`.
- Walk records by `(device_id, stream_id, sequence_number)`.
- Recompute hashes.
- Check signatures.
- Check previous-hash continuity.
- Return first broken record.

## Phase 5: Dashboard

- Show latest records.
- Add a verify button.
- Show valid or broken-chain result.

## Demo Acceptance Criteria

The demo is successful only if tampering with a stored record causes verification to fail and points to the broken sequence number.
