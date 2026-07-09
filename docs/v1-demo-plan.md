# AegisEye V1 Demo Plan

## Demo Objective

Show that AegisEye can create a redacted evidence payload, hash it, sign it, store it, and detect tampering.

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

Status: in progress

- Define evidence payload schema.
- Define ledger database table.
- Create local PostgreSQL Compose setup.
- Create edge prototype for canonical payload, hash, and signature.

## Phase 2: Edge Computer Vision

- Read a sample video.
- Detect faces with a simple OpenCV detector or ONNX model.
- Redact detected faces.
- Hash redacted frame output.
- Emit one payload per frame batch.

## Phase 3: Backend Ingestion

- Create `POST /api/evidence`.
- Validate payload schema.
- Recompute payload hash.
- Verify edge signature.
- Persist record.

## Phase 4: Chain Verification

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
