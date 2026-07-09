# AegisEye Contracts

This folder contains shared data contracts used across edge, backend, and frontend services.

The most important file is `evidence-payload.schema.json`. It defines the canonical evidence payload that is hashed and signed by the edge bridge.

## Contract Rules

- The edge bridge must serialize payloads canonically before hashing.
- The backend must verify the hash against the exact canonical payload it stores.
- `sequenceNumber` must increase by one for each stream.
- `previousPayloadHash` must match the prior accepted ledger record for the same `deviceId` and `streamId`.
- The signature must cover the `payloadHash` for v1.

## Why This Matters

The project's main guarantee depends on all services agreeing on the exact payload shape. If two services serialize the same logical data differently, the hash chain becomes unreliable.
