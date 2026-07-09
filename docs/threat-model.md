# AegisEye V1 Threat Model

## Security Goal

For v1, AegisEye must detect whether a stored evidence record was changed after it was accepted.

The system is not yet claiming production-grade custody, legal admissibility, or protection against a fully compromised edge device.

## Assets

- Canonical evidence payload
- Payload hash
- Previous payload hash
- Edge signing key
- Signature
- Ledger rows
- Device identity

## Trust Boundaries

```text
edge device -> backend ingestion -> database -> dashboard/user
```

The highest-risk boundary is between the edge device and the backend. The backend must not trust incoming payloads until schema validation, hash verification, signature verification, and sequence checks pass.

## V1 Threats

### Tampered Database Row

An attacker changes `canonical_payload`, `payload_hash`, `previous_payload_hash`, or `signature` in PostgreSQL.

Mitigation:

- Verify the payload hash from the stored canonical payload.
- Verify the signature against the stored payload hash.
- Verify each `previous_payload_hash` points to the prior record.

### Missing Ledger Record

An attacker deletes a row from the middle of a stream chain.

Mitigation:

- Enforce monotonic `sequence_number`.
- Verify adjacent records by sequence.
- Report gaps.

### Replay

An attacker resubmits an old valid payload.

Mitigation:

- Enforce unique `(device_id, stream_id, sequence_number)`.
- Later: include nonce windows or backend-issued session tokens.

### Fake Edge Device

An attacker sends payloads as an unknown device.

Mitigation:

- Require known `device_id`.
- Require a known `public_key_id`.
- Verify signatures.

### Compromised Demo Key

An attacker obtains the v1 filesystem private key.

Mitigation:

- V1 documents this as a known limitation.
- Production must use TPM/HSM-backed storage, key rotation, and revocation.

## Out Of Scope For V1

- Preventing compromise of a physical edge device
- Formal compliance certification
- Proving redaction never misses a face
- Multi-tenant authorization
- Long-term archival integrity
- External notarization or Merkle anchoring
