from __future__ import annotations

import json
from datetime import UTC, datetime
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


def build_payload(redacted_frame_hash: str) -> dict:
    return {
        "schemaVersion": "aegis.evidence.v1",
        "deviceId": "edge-demo-001",
        "streamId": "camera-demo-001",
        "sequenceNumber": 1,
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
        "previousPayloadHash": None,
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

    payload = build_payload(redacted_hash)
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


if __name__ == "__main__":
    main()
