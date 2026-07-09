from __future__ import annotations

import json
import os
from datetime import UTC, datetime
from urllib import request
from uuid import uuid4

import cv2
import numpy as np

from .canonical import canonical_json_bytes, payload_hash, sha256_hex
from .crypto import generate_demo_private_key, public_key_hex, sign_hash, verify_hash_signature


def synthetic_frame() -> np.ndarray:
    frame = np.full((360, 640, 3), 235, dtype=np.uint8)
    cv2.rectangle(frame, (220, 80), (420, 300), (80, 120, 180), thickness=-1)
    cv2.circle(frame, (320, 155), 55, (185, 165, 140), thickness=-1)
    cv2.rectangle(frame, (265, 215), (375, 300), (60, 80, 130), thickness=-1)
    return frame


def redact_box(frame: np.ndarray, x: int, y: int, width: int, height: int) -> np.ndarray:
    redacted = frame.copy()
    roi = redacted[y : y + height, x : x + width]
    if roi.size == 0:
        raise ValueError("redaction box is outside the frame")
    blurred = cv2.GaussianBlur(roi, (35, 35), 0)
    redacted[y : y + height, x : x + width] = blurred
    return redacted


def frame_hash(frame: np.ndarray) -> str:
    ok, encoded = cv2.imencode(".jpg", frame, [int(cv2.IMWRITE_JPEG_QUALITY), 90])
    if not ok:
        raise RuntimeError("failed to encode redacted frame")
    return sha256_hex(encoded.tobytes())


def build_payload(
    redacted_frame_hash: str,
    device_id: str,
    stream_id: str,
    sequence_number: int,
    previous_payload_hash: str | None,
) -> dict:
    return {
        "schemaVersion": "aegis.evidence.v1",
        "deviceId": device_id,
        "streamId": stream_id,
        "sequenceNumber": sequence_number,
        "capturedAt": datetime.now(UTC).isoformat().replace("+00:00", "Z"),
        "redactionModel": {
            "name": "synthetic-face-redactor",
            "version": "0.1.0",
            "mode": "synthetic-demo",
        },
        "frameBatch": {
            "batchId": str(uuid4()),
            "startFrame": 0,
            "endFrame": 0,
            "frameCount": 1,
            "redactedFrameHash": redacted_frame_hash,
        },
        "previousPayloadHash": previous_payload_hash,
        "detections": [
            {
                "type": "face",
                "confidence": 0.99,
                "redaction": "blur",
                "box": {
                    "x": 265,
                    "y": 100,
                    "width": 110,
                    "height": 110,
                },
            }
        ],
    }


def main() -> None:
    detection = {"x": 265, "y": 100, "width": 110, "height": 110}
    frame = synthetic_frame()
    redacted = redact_box(frame, **detection)
    redacted_hash = frame_hash(redacted)

    previous_payload_hash = os.getenv("AEGIS_PREVIOUS_PAYLOAD_HASH")
    payload = build_payload(
        redacted_hash,
        device_id=os.getenv("AEGIS_DEVICE_ID", "edge-demo-001"),
        stream_id=os.getenv("AEGIS_STREAM_ID", "camera-demo-001"),
        sequence_number=int(os.getenv("AEGIS_SEQUENCE_NUMBER", "1")),
        previous_payload_hash=previous_payload_hash if previous_payload_hash else None,
    )
    hash_hex = payload_hash(payload)

    private_key = generate_demo_private_key()
    public_key = private_key.public_key()
    signature = sign_hash(private_key, hash_hex)
    signature_valid = verify_hash_signature(public_key, hash_hex, signature)

    record = {
        "payloadHash": hash_hex,
        "previousPayloadHash": payload["previousPayloadHash"],
        "signature": signature,
        "signatureAlgorithm": "ed25519",
        "publicKeyId": "demo-ed25519-key",
        "publicKeyHex": public_key_hex(public_key),
        "signatureValid": signature_valid,
        "canonicalPayload": json.loads(canonical_json_bytes(payload)),
    }

    print(json.dumps(record, indent=2, sort_keys=True))

    ingest_url = os.getenv("AEGIS_INGEST_URL")
    if ingest_url:
        body = json.dumps(record, sort_keys=True).encode("utf-8")
        http_request = request.Request(
            ingest_url,
            data=body,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        with request.urlopen(http_request, timeout=10) as response:
            response_body = response.read().decode("utf-8")
        print(response_body)


if __name__ == "__main__":
    main()
