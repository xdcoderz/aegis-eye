# AegisEye Hackathon Submission Brief

## One-Line Pitch

AegisEye is a privacy-preserving video evidence system that redacts faces at the edge and cryptographically identifies any evidence record changed after capture.

## Problem

Video evidence creates two competing risks:

- Sending raw footage to centralized systems exposes identities and increases breach impact.
- Redacted or processed footage can be challenged if no one can prove when or where it was modified.

Conventional computer-vision demos address detection. Conventional ledgers address data integrity. AegisEye joins both around the evidence lifecycle.

## Solution

The edge bridge detects and blurs faces before creating evidence metadata. It hashes the redacted frame, builds a canonical payload, links it to the previous payload, and signs the hash with an Ed25519 device key. The backend independently verifies and stores each record. A reviewer can later re-run the entire chain and locate the first modification, missing sequence, bad signature, or broken link.

## Demonstrated Innovation

```text
Privacy at capture + cryptographic device origin + chained custody + explainable failure location
```

The novelty is the integrated trust workflow, not a claim to have invented face detection, SHA-256, digital signatures, or hash chains individually.

## Target Users

- Public safety and municipal camera operators
- Hospitals and care facilities handling sensitive footage
- Industrial safety teams
- Transport and public-infrastructure operators
- Investigators reviewing exported redacted evidence

## Prototype Capabilities

| Capability | Demonstrated behavior |
|---|---|
| Privacy redaction | OpenCV detects frontal faces and blurs them on webcam or video input |
| Device identity | Persistent Ed25519 key is paired to a registered demo device |
| Evidence integrity | Canonical payload and redacted frame are SHA-256 hashed |
| Chain of custody | Every non-genesis payload references the prior payload hash |
| Secure ingestion | Hash, signature, identity, sequence, and link are checked before insert |
| Post-storage audit | Stored rows are independently re-hashed and re-verified |
| Explainable detection | API and console report the first broken sequence and reason |
| Live adversarial demo | A controlled endpoint mutates one demo row without updating its signature |

## Judge Demo

1. Generate a five-record signed stream.
2. Verify it as valid in the browser console.
3. Intentionally mutate sequence 3.
4. Verify again and show `BROKEN at sequence 3`.
5. Run the webcam path to show face redaction and signed evidence emission.

The synthetic stream is the deterministic trust demo. The webcam run is the real computer-vision demo. They use the same evidence and verification path.

## Technical Stack

- Python, OpenCV, NumPy, and `cryptography` on the edge
- Java 21 and Spring Boot for APIs and verification
- PostgreSQL for the evidence ledger and device registry
- HTML, CSS, and JavaScript for the console
- Docker Compose for local infrastructure
- JSON Schema for the shared evidence contract

## Value

### Social value

Reduce unnecessary identity exposure while preserving the ability to audit whether evidence changed.

### Technical value

Demonstrate cross-language canonicalization, applied cryptography, computer vision, sequence integrity, secure API validation, and explainable verification in one system.

### Product value

Create a foundation for privacy-sensitive evidence workflows in which operators need both usable footage and verifiable custody.

## Honest Boundaries

- Haar detection can miss faces and is a prototype detector.
- First-run key pairing is trust on first use.
- Private keys are filesystem-based, not TPM/HSM-backed.
- The ledger is tamper-evident, not immutable.
- Tail deletion requires an external chain-head anchor to prove.
- No authentication, multi-tenancy, cloud deployment, or legal-admissibility claim is included.

These boundaries are documented design decisions and form the pilot roadmap.

## Evaluation Alignment

- Innovation: connects edge privacy and cryptographic evidence verification.
- Technical depth: computer vision, canonical data contracts, signatures, database constraints, chain verification, and a working UI.
- Impact: applicable where raw visual data is sensitive but evidence integrity matters.
- Feasibility: the complete claim runs locally with commodity hardware and open-source components.
- Demonstrability: valid-to-tampered state transition is repeatable in under three minutes.
- Scalability path: service boundaries and the phase roadmap identify enrollment, object storage, anchoring, and operations work.

## End Goal

Move from a local prototype to a pilot in which multiple authenticated edge devices produce encrypted redacted media and independently anchored evidence chains, with measurable privacy performance and controlled reviewer access.
