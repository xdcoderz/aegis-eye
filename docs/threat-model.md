# AegisEye Prototype Threat Model

## Security Goal

Detect whether accepted evidence was modified, deleted, reordered, replayed, or attributed to the wrong paired demo key, while reducing exposure of detected faces through edge redaction.

The prototype does not claim that a compromised camera is trustworthy, that redaction is perfect, or that evidence is legally admissible.

## Protected Assets

- Edge private signing key
- Paired public device identity
- Redacted frame integrity hash
- Canonical evidence payload
- Sequence and previous-payload link
- Ed25519 signature
- Ledger rows and verification results

## Threats and Controls

| Threat | Prototype control | Residual risk |
|---|---|---|
| Canonical payload changed in PostgreSQL | Recompute SHA-256 from stored payload | An attacker controlling the key registry and ledger could replace both |
| Stored hash changed | Recompute hash and verify signature | Database is not externally anchored |
| Signature changed | Verify Ed25519 against paired public key | Filesystem private key is not hardware-backed |
| Middle row deleted | Require contiguous sequence and previous-hash links | Deleting a tail is not distinguishable without an external expected head |
| Row reordered | Sort and validate monotonic sequence and links | Verification depends on the selected stream identity |
| Old record replayed | Unique device/stream/sequence and payload-hash constraints | No backend nonce or session token yet |
| Unknown device sends evidence | Pre-registered active device and key ID | Initial demo public key pairing is trust on first use |
| Different key impersonates paired device | Exact public-key match after pairing | Enrollment is not authenticated in the prototype |
| Face visible outside redaction box | Edge blur before output | Haar detection can miss pose, occlusion, or poor lighting |
| Tamper endpoint abused | Restrict to `demo-*`; configurable disable flag | No user authentication in local prototype |

## Trust-on-First-Use Pairing

The seeded demo device initially has no public key. Its first valid signed submission stores the supplied Ed25519 public key. Later submissions must use the exact same key. This is adequate for a controlled hackathon environment, but production enrollment must authenticate the device before binding a key.

## Important Integrity Limitation

The chain detects deletion in the middle because subsequent records point to the missing hash. It cannot prove that the latest tail record was deleted unless an expected chain head or record count is anchored outside the same database. A production design should periodically anchor signed chain heads to an independent timestamping or transparency service.

## Privacy Limitations

- Haar face detection is not a guarantee of complete face coverage.
- Redaction quality has not been benchmarked against re-identification attacks.
- The local redacted MP4 and evidence manifest still require access control and retention policies.
- Metadata such as timestamps and device IDs can itself be sensitive.

## Production Requirements

- Authenticated device enrollment and rotation
- TPM or HSM-backed private keys
- Historical public-key registry with revocation periods
- Authenticated users and role-based actions
- Disable or remove the controlled tamper API
- Encrypted transport, storage, and backups
- Independent chain-head anchoring
- Redaction accuracy and privacy evaluation
- Security review and jurisdiction-specific legal assessment
