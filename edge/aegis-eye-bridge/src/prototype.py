from __future__ import annotations

import os
from pathlib import Path

import cv2
import numpy as np

from .canonical import sha256_hex
from .crypto import load_or_create_private_key
from .evidence import build_payload, create_signed_record, post_record


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


def main() -> None:
    detection = {"x": 265, "y": 100, "width": 110, "height": 110}
    frame = synthetic_frame()
    redacted = redact_box(frame, **detection)
    redacted_hash = frame_hash(redacted)

    previous_payload_hash = os.getenv("AEGIS_PREVIOUS_PAYLOAD_HASH")
    payload = build_payload(
        redacted_frame_hash=redacted_hash,
        detections=[
            {
                "type": "face",
                "confidence": 0.99,
                "redaction": "blur",
                "box": detection,
            }
        ],
        model_name="synthetic-face-redactor",
        model_version="0.2.0",
        model_mode="synthetic-demo",
        device_id=os.getenv("AEGIS_DEVICE_ID", "edge-demo-001"),
        stream_id=os.getenv("AEGIS_STREAM_ID", "demo-synthetic"),
        sequence_number=int(os.getenv("AEGIS_SEQUENCE_NUMBER", "1")),
        previous_payload_hash=previous_payload_hash if previous_payload_hash else None,
        start_frame=0,
        end_frame=0,
        frame_count=1,
    )
    key_path = Path(os.getenv("AEGIS_KEY_PATH", "keys/demo-device-ed25519.pem"))
    private_key = load_or_create_private_key(key_path)
    record = create_signed_record(payload, private_key)

    artifact_dir = Path(os.getenv("AEGIS_ARTIFACT_DIR", "artifacts"))
    artifact_dir.mkdir(parents=True, exist_ok=True)
    cv2.imwrite(str(artifact_dir / "synthetic-redacted.jpg"), redacted)
    print(f"Created signed evidence {record['payloadHash']} from a redacted synthetic frame")

    ingest_url = os.getenv("AEGIS_INGEST_URL")
    if ingest_url:
        response = post_record(ingest_url, record)
        print(f"Gateway accepted ledger record {response['ledgerId']}")


if __name__ == "__main__":
    main()
