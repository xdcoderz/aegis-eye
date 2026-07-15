from __future__ import annotations

import json
from datetime import UTC, datetime
from typing import Any
from urllib import error, request
from uuid import uuid4

from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey

from .canonical import canonical_json_bytes, payload_hash
from .crypto import public_key_hex, sign_hash, verify_hash_signature


def build_payload(
    *,
    redacted_frame_hash: str,
    detections: list[dict[str, Any]],
    model_name: str,
    model_version: str,
    model_mode: str,
    device_id: str,
    stream_id: str,
    sequence_number: int,
    previous_payload_hash: str | None,
    start_frame: int,
    end_frame: int,
    frame_count: int,
    captured_at: datetime | None = None,
) -> dict[str, Any]:
    captured = captured_at or datetime.now(UTC)
    return {
        "schemaVersion": "aegis.evidence.v1",
        "deviceId": device_id,
        "streamId": stream_id,
        "sequenceNumber": sequence_number,
        "capturedAt": captured.isoformat().replace("+00:00", "Z"),
        "redactionModel": {
            "name": model_name,
            "version": model_version,
            "mode": model_mode,
        },
        "frameBatch": {
            "batchId": str(uuid4()),
            "startFrame": start_frame,
            "endFrame": end_frame,
            "frameCount": frame_count,
            "redactedFrameHash": redacted_frame_hash,
        },
        "previousPayloadHash": previous_payload_hash,
        "detections": detections,
    }


def create_signed_record(
    payload: dict[str, Any],
    private_key: Ed25519PrivateKey,
    public_key_id: str = "demo-ed25519-key",
) -> dict[str, Any]:
    hash_hex = payload_hash(payload)
    public_key = private_key.public_key()
    signature = sign_hash(private_key, hash_hex)
    if not verify_hash_signature(public_key, hash_hex, signature):
        raise RuntimeError("locally generated evidence signature did not verify")

    return {
        "payloadHash": hash_hex,
        "previousPayloadHash": payload["previousPayloadHash"],
        "signature": signature,
        "signatureAlgorithm": "ed25519",
        "publicKeyId": public_key_id,
        "publicKeyHex": public_key_hex(public_key),
        "canonicalPayload": json.loads(canonical_json_bytes(payload)),
    }


def post_record(ingest_url: str, record: dict[str, Any]) -> dict[str, Any]:
    body = json.dumps(record, sort_keys=True).encode("utf-8")
    http_request = request.Request(
        ingest_url,
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with request.urlopen(http_request, timeout=15) as response:
            return json.loads(response.read().decode("utf-8"))
    except error.HTTPError as exception:
        response_body = exception.read().decode("utf-8")
        raise RuntimeError(f"gateway rejected evidence ({exception.code}): {response_body}") from exception
