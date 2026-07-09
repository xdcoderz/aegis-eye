CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS edge_device (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id VARCHAR(128) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    public_key_id VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT edge_device_status_check CHECK (status IN ('active', 'revoked', 'disabled'))
);

CREATE TABLE IF NOT EXISTS evidence_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id VARCHAR(128) NOT NULL,
    stream_id VARCHAR(128) NOT NULL,
    sequence_number BIGINT NOT NULL,
    schema_version VARCHAR(64) NOT NULL,
    captured_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    payload_hash CHAR(64) NOT NULL,
    previous_payload_hash CHAR(64),
    signature TEXT NOT NULL,
    signature_algorithm VARCHAR(64) NOT NULL DEFAULT 'ed25519',
    public_key_id VARCHAR(128) NOT NULL,
    canonical_payload JSONB NOT NULL,
    verification_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    verification_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT evidence_ledger_hash_check CHECK (payload_hash ~ '^[a-f0-9]{64}$'),
    CONSTRAINT evidence_ledger_prev_hash_check CHECK (
        previous_payload_hash IS NULL OR previous_payload_hash ~ '^[a-f0-9]{64}$'
    ),
    CONSTRAINT evidence_ledger_verification_status_check CHECK (
        verification_status IN ('pending', 'valid', 'invalid')
    ),
    CONSTRAINT evidence_ledger_sequence_unique UNIQUE (device_id, stream_id, sequence_number),
    CONSTRAINT evidence_ledger_payload_hash_unique UNIQUE (payload_hash)
);

CREATE INDEX IF NOT EXISTS idx_evidence_ledger_stream_sequence
    ON evidence_ledger (device_id, stream_id, sequence_number);

CREATE INDEX IF NOT EXISTS idx_evidence_ledger_received_at
    ON evidence_ledger (received_at DESC);

CREATE INDEX IF NOT EXISTS idx_evidence_ledger_payload_hash
    ON evidence_ledger (payload_hash);

INSERT INTO edge_device (device_id, display_name, public_key_id)
VALUES ('edge-demo-001', 'Demo Edge Device', 'demo-ed25519-key')
ON CONFLICT (device_id) DO NOTHING;
