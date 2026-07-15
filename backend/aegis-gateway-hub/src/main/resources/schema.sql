ALTER TABLE edge_device
    ADD COLUMN IF NOT EXISTS public_key_hex CHAR(64);

ALTER TABLE edge_device
    ADD COLUMN IF NOT EXISTS paired_at TIMESTAMPTZ;
