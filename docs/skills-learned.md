# Skills Learned By Building AegisEye

## Computer Vision Skills

AegisEye teaches computer vision as a deployed system, not just a notebook.

You will learn:

- how to read frames from video,
- how to manage frame batches,
- how face detection works in real footage,
- how confidence thresholds affect privacy,
- how to blur, pixelate, or mask detected regions,
- how lighting, blur, camera angle, and occlusion affect detection,
- how to hash redacted visual output for later verification.

## Backend Skills

You will learn:

- building ingestion APIs,
- validating incoming records,
- persisting evidence metadata,
- designing database tables for auditability,
- handling duplicate records,
- returning useful verification errors.

## Cryptography And Security Skills

You will learn practical applied security:

- canonical JSON,
- SHA-256 hashes,
- hash chains,
- Ed25519 signatures,
- signature verification,
- public/private key roles,
- why demo key storage is not production key custody,
- how tampering can be detected.

## System Design Skills

You will learn how to connect different parts of a real system:

- edge producer,
- backend ingestion,
- relational ledger,
- verification service,
- dashboard consumer.

## Portfolio Value

AegisEye is valuable as a portfolio project because it combines AI, backend, security, and product thinking into one coherent system.

The strongest demo is not "look, it detects faces." The strongest demo is:

> The system redacts visual evidence, signs it, stores it, and catches tampering.
