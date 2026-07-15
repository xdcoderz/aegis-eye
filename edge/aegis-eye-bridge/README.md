# aegis-eye-bridge

Python/OpenCV edge producer for AegisEye Core.

It redacts faces, hashes redacted frames, builds linked canonical payloads, signs them with a persistent Ed25519 identity, and submits signed evidence to the gateway.

## Real Webcam or Video

```powershell
$stream = "demo-webcam-" + (Get-Date -Format "yyyyMMdd-HHmmss")
.\.venv\Scripts\python.exe -m src.video_pipeline --source 0 --stream-id $stream --display
```

Use a video path instead of `0` for file input. The default run processes 180 frames and emits evidence every 30 frames.

Outputs under `artifacts/`:

- `redacted-output.mp4`: processed video containing blurred detections
- `evidence-records.jsonl`: signed evidence envelopes

Original frames are processed in memory and are not written by the bridge.

## Reliable Synthetic Demo

```powershell
.\.venv\Scripts\python.exe -m src.demo_seed
```

This creates a unique five-record `demo-synthetic-*` stream, submits it, and saves a redacted preview and JSONL manifest. It exercises the same signing, ingestion, ledger, and verification path as the real video producer.

## Single-Record Development Probe

```powershell
$env:AEGIS_INGEST_URL='http://localhost:8080/api/evidence'
$env:AEGIS_STREAM_ID='demo-probe-001'
.\.venv\Scripts\python.exe -m src.prototype
```

## Device Identity

The edge key is created at `keys/demo-device-ed25519.pem` on first use and reused. The key directory is ignored by git. The backend pairs the corresponding public key to `edge-demo-001` on the first accepted record.

This is local prototype custody. Production requires authenticated enrollment, TPM/HSM-backed keys, rotation, and revocation.

## Useful Video Options

```text
--source PATH_OR_CAMERA
--output PATH
--stream-id ID
--ingest-url URL
--evidence-every N
--max-frames N
--display
--no-submit
```

## Tests

```powershell
.\.venv\Scripts\python.exe -m unittest discover -s tests
```
