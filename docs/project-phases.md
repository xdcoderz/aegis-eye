# AegisEye Project Phases

## End Goal

AegisEye Core should become a privacy-preserving, tamper-evident video evidence platform.

The final system should:

- redact sensitive visual data near the camera,
- create canonical evidence payloads,
- hash and sign each payload,
- store the payloads in an append-oriented ledger,
- verify the chain of custody,
- detect missing or modified records,
- give operators a dashboard for evidence integrity.

## Why We Are Building USP First

The most important value of this project is not "camera monitoring." Many projects can show camera monitoring.

The unique selling point is:

> AegisEye can prove whether redacted visual evidence was changed after ingestion.

That proof must exist before we build a polished dashboard, multi-camera support, or advanced AI.

## Phase 0: Foundation

Goal:

Create the repo structure, shared contract, database schema, and local development environment.

Deliverables:

- monorepo structure,
- evidence payload JSON Schema,
- PostgreSQL Compose setup,
- ledger table,
- edge hash/sign prototype,
- initial docs.

Status:

Complete.

## Phase 1: USP Proof

Goal:

Prove the core trust loop.

Deliverables:

- edge prototype emits signed evidence records,
- gateway verifies payload hash and signature,
- gateway stores valid records,
- invalid records are rejected,
- database contains enough data to verify later.

Success criteria:

- a valid edge record inserts into `evidence_ledger`,
- a changed payload hash is rejected,
- a changed signature is rejected.

Status:

Complete for the first ingestion path. The next phase is stored-chain verification.

## Phase 2: MVP Chain Verification

Goal:

Detect tampering after records are stored.

Deliverables:

- multiple sequential records,
- `previousPayloadHash` continuity,
- verification endpoint,
- gap detection,
- first-broken-record reporting.

Success criteria:

- modifying one ledger row causes verification to fail,
- the response identifies the broken `sequenceNumber`.

## Phase 3: Computer Vision MVP

Goal:

Replace the synthetic demo frame with real video processing.

Deliverables:

- sample video ingestion,
- face detection,
- face redaction,
- redacted frame hashing,
- payload emission per frame batch.

Success criteria:

- the system processes a sample video,
- redacted output is visibly privacy-preserving,
- generated evidence records still verify.

## Phase 4: Dashboard MVP

Goal:

Make the MVP understandable to reviewers and teammates.

Deliverables:

- ledger list,
- latest device status,
- verify-chain button,
- broken-record display.

Success criteria:

- a user can run the demo without reading database rows manually.

## Phase 5: Complete Project Track

Goal:

Move from portfolio MVP toward a realistic product architecture.

Deliverables:

- multi-camera support,
- device registration,
- public key registry,
- key rotation and revocation,
- replay protection,
- evidence export,
- audit trail,
- deployment documentation,
- security review checklist.

This phase should start only after the MVP proves tamper detection end to end.

## Skills Learned

### Computer Vision

- video frame capture,
- face detection,
- bounding box handling,
- redaction quality tradeoffs,
- frame hashing,
- practical model deployment constraints.

### Backend Engineering

- REST API design,
- input validation,
- database schema design,
- PostgreSQL persistence,
- error handling,
- service boundaries.

### Security Engineering

- canonical serialization,
- SHA-256 hashing,
- Ed25519 signatures,
- signature verification,
- replay and tamper threat modeling,
- key custody limitations.

### Distributed Systems

- edge-to-server contracts,
- sequence numbers,
- idempotency,
- append-oriented ledger design,
- verification workflows.

### Product Thinking

- USP-first scoping,
- MVP definition,
- demo design,
- tradeoffs between proof, polish, and production readiness.
