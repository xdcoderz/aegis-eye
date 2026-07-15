# AegisEye Core

AegisEye Core is a hackathon-ready prototype for privacy-preserving, tamper-evident video evidence.

It proves one focused claim end to end:

```text
camera/video -> face redaction at the edge -> canonical payload -> SHA-256 hash
-> Ed25519 signature -> chained PostgreSQL ledger -> verification -> tamper localization
```

## Prototype Status

The hackathon MVP is implemented.

- Real webcam or video-file processing with OpenCV face detection and blur redaction
- Persistent Ed25519 edge identity with first-run public-key pairing
- Canonical evidence payloads and SHA-256 frame/payload hashes
- Strict sequence and previous-hash checks during ingestion
- PostgreSQL evidence ledger
- Full stored-chain verification of metadata, hashes, signatures, order, and links
- Intentional tamper simulation restricted to `demo-*` streams
- Browser-based evidence integrity console served by the backend
- Synthetic fallback runner for a reliable no-camera demo

The prototype is suitable for a hackathon submission. It is not yet a production surveillance system or a claim of legal admissibility.

## Why It Matters

Most computer-vision demos stop after detection. AegisEye connects computer vision to evidence trust:

- Privacy: faces are redacted before evidence leaves the edge process.
- Authenticity: each payload is signed by a paired device key.
- Integrity: stored payloads are re-hashed and signatures are re-verified.
- Continuity: every record points to the previous signed payload hash.
- Explainability: a broken chain reports the first affected sequence number and reason.

## Repository Layout

```text
backend/aegis-gateway-hub/   Spring Boot API, ledger verifier, and web console
contracts/                   Evidence payload JSON Schema
docs/                        Architecture, phases, demo, threat model, submission notes
edge/aegis-eye-bridge/       OpenCV redaction and signed evidence producers
infra/                       PostgreSQL Docker Compose setup
```

## Requirements

- Docker Desktop
- Python 3.11+
- Java 21 and Maven
- A webcam or sample video for the real CV path

## Quick Start

Start PostgreSQL from the repository root:

```powershell
docker compose -f .\infra\docker-compose.yml up -d
```

Start the gateway from a second terminal:

```powershell
cd .\backend\aegis-gateway-hub
mvn spring-boot:run
```

Open the console at [http://localhost:8080](http://localhost:8080).

Create a reliable five-record demo stream from a third terminal:

```powershell
cd .\edge\aegis-eye-bridge
.\.venv\Scripts\python.exe -m src.demo_seed
```

Refresh the console, select the newest `demo-synthetic-*` stream, and run `Verify chain`. Then tamper with a sequence and verify again. The console identifies the first broken record.

## Real Computer-Vision Run

Webcam input:

```powershell
cd .\edge\aegis-eye-bridge
$stream = "demo-webcam-" + (Get-Date -Format "yyyyMMdd-HHmmss")
.\.venv\Scripts\python.exe -m src.video_pipeline --source 0 --stream-id $stream --display
```

Video-file input:

```powershell
$stream = "demo-video-" + (Get-Date -Format "yyyyMMdd-HHmmss")
.\.venv\Scripts\python.exe -m src.video_pipeline --source "D:\path\sample.mp4" --stream-id $stream
```

The edge process writes a redacted MP4 and JSONL evidence manifest under `edge/aegis-eye-bridge/artifacts/`. Original frames are processed in memory and are not written by AegisEye.

## Tests

```powershell
cd .\edge\aegis-eye-bridge
.\.venv\Scripts\python.exe -m unittest discover -s tests

cd ..\..\backend\aegis-gateway-hub
mvn test
```

## Documentation

- [Hackathon submission brief](docs/hackathon-submission.md)
- [Project phases and roadmap](docs/project-phases.md)
- [Architecture](docs/architecture.md)
- [Demo runbook](docs/v1-demo-plan.md)
- [Threat model](docs/threat-model.md)
- [Skills learned](docs/skills-learned.md)
- [API reference](docs/api.md)

## End Goal

The long-term goal is an independently auditable evidence platform that redacts sensitive visual data near capture, binds evidence to registered device identities, detects modification or deletion, and supports controlled evidence review and export. The current prototype establishes the core technical trust loop on which that product can be built.
