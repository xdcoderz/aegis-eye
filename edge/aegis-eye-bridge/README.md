# aegis-eye-bridge

Python edge prototype for AegisEye Core.

The bridge is responsible for:

- reading video frames,
- detecting sensitive visual regions,
- redacting those regions,
- creating canonical evidence payloads,
- hashing payloads,
- signing hashes,
- sending signed evidence records to the backend.

## Current Prototype

The current prototype does not require a real camera. It creates a synthetic frame, simulates one face detection, redacts the region, hashes the redacted frame, builds an evidence payload, signs the payload hash with a demo Ed25519 key, verifies the signature, and prints the result.

Run it from this folder:

```powershell
.\.venv\Scripts\python.exe -m src.prototype
```

Or from the repo root:

```powershell
cd .\edge\aegis-eye-bridge
.\.venv\Scripts\python.exe -m src.prototype
```

## Important

The demo key is generated at runtime. It is not production key custody. Production needs hardware-backed key storage, rotation, and revocation.
