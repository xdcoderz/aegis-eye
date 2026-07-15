# AegisEye Project Phases

## End Goal

Build an independently auditable, privacy-preserving video evidence platform. Sensitive regions are redacted near capture, every evidence payload is signed by a registered edge identity, records form a tamper-evident chain, and authorized reviewers can verify custody before using or exporting evidence.

## Scope Strategy

AegisEye is organized around increasingly strong claims:

1. USP: prove that redacted computer-vision evidence can be cryptographically verified.
2. Hackathon MVP: make that proof visible, repeatable, and understandable in a live demo.
3. Pilot: test multiple real devices and realistic failure conditions.
4. Product: add production identity, access, storage, deployment, and compliance controls.

## Phase 0: Foundation

Status: complete

- Monorepo and service boundaries
- Shared evidence JSON Schema
- Dockerized PostgreSQL
- Canonical JSON, SHA-256, and Ed25519 primitives
- Initial threat model and architecture

Exit criterion: the same payload produces the same hash and a valid signature can be distinguished from an invalid one.

## Phase 1: USP Ingestion Proof

Status: complete

- Signed evidence ingestion endpoint
- Server-side payload-hash recomputation
- Ed25519 signature verification
- Registered device and key-ID check
- PostgreSQL ledger persistence
- Duplicate sequence and payload rejection

Exit criterion: valid evidence is stored; modified payloads and signatures are rejected.

## Phase 2: Chain-of-Custody MVP

Status: complete

- Persistent edge signing identity
- First-run demo key pairing
- Strict first-record, sequence, and previous-hash rules
- Full stored-ledger verification endpoint
- Gap, metadata, hash, link, and signature checks
- First-broken-sequence reporting

Exit criterion: changing a stored canonical payload makes verification fail at the affected sequence.

## Phase 3: Computer-Vision MVP

Status: complete

- Webcam and video-file capture
- OpenCV Haar face detection
- Edge blur redaction
- Redacted-frame hashing
- Linked evidence emission at configurable frame intervals
- Redacted MP4 and JSONL evidence manifest
- No-camera synthetic fallback

Exit criterion: a real video source produces visibly redacted output and a valid evidence chain.

## Phase 4: Hackathon Experience

Status: complete

- Browser evidence integrity console
- Stream and ledger record views
- Chain verification control and result display
- Controlled tamper simulation for `demo-*` streams
- Setup, demo, architecture, API, threat, and submission documentation

Exit criterion: a judge can understand the value and witness valid-to-broken verification in under three minutes.

This is the hackathon submission boundary.

## Phase 5: Pilot Hardening

Status: planned

- Stronger face detector such as a reviewed ONNX model
- Detection-quality evaluation across lighting, pose, and occlusion
- Explicit device enrollment workflow
- Key rotation, historical key registry, and revocation
- Authentication and role-based access
- Object storage for encrypted redacted media
- Database migrations and integration-test environment
- Containerized gateway and reproducible deployment
- Metrics, tracing, health checks, and structured audit events

Exit criterion: multiple registered devices can operate for a sustained trial with measurable redaction and verification reliability.

## Phase 6: Product Track

Status: future

- TPM or HSM-backed edge keys
- Secure boot and remote device attestation
- Multi-tenant authorization
- Retention and evidence export policies
- External timestamp or Merkle anchoring
- Disaster recovery and archival verification
- Security review, privacy assessment, and relevant legal/compliance validation

Exit criterion: claims are backed by production controls, independent review, and operational evidence.

## Submission Decision

The project can now be submitted as a prototype because the differentiating claim is implemented end to end, is visually demonstrable, has a fallback demo path, and clearly separates proven behavior from future production work.
