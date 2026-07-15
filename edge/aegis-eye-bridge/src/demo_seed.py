from __future__ import annotations

import argparse
import json
from datetime import UTC, datetime
from pathlib import Path

import cv2

from .crypto import load_or_create_private_key
from .evidence import build_payload, create_signed_record, post_record
from .prototype import frame_hash, redact_box, synthetic_frame


def build_parser() -> argparse.ArgumentParser:
    timestamp = datetime.now(UTC).strftime("%Y%m%d-%H%M%S")
    parser = argparse.ArgumentParser(description="Create a repeatable AegisEye hackathon demo stream")
    parser.add_argument("--records", type=int, default=5)
    parser.add_argument("--stream-id", default=f"demo-synthetic-{timestamp}")
    parser.add_argument("--device-id", default="edge-demo-001")
    parser.add_argument("--key", default="keys/demo-device-ed25519.pem")
    parser.add_argument("--ingest-url", default="http://localhost:8080/api/evidence")
    parser.add_argument("--no-submit", action="store_true")
    parser.add_argument("--artifact-dir", default="artifacts")
    return parser


def main() -> None:
    args = build_parser().parse_args()
    if args.records < 1:
        raise ValueError("--records must be at least 1")

    private_key = load_or_create_private_key(Path(args.key))
    artifact_dir = Path(args.artifact_dir)
    artifact_dir.mkdir(parents=True, exist_ok=True)
    manifest_path = artifact_dir / f"{args.stream_id}-records.jsonl"
    detection_box = {"x": 265, "y": 100, "width": 110, "height": 110}
    previous_payload_hash: str | None = None

    with manifest_path.open("w", encoding="utf-8") as manifest:
        for index in range(args.records):
            frame = synthetic_frame()
            cv2.putText(
                frame,
                f"frame {index + 1}",
                (24, 334),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.65,
                (25, 35, 30),
                2,
            )
            redacted = redact_box(frame, **detection_box)
            payload = build_payload(
                redacted_frame_hash=frame_hash(redacted),
                detections=[{
                    "type": "face",
                    "confidence": 0.99,
                    "redaction": "blur",
                    "box": detection_box,
                }],
                model_name="synthetic-face-redactor",
                model_version="0.2.0",
                model_mode="synthetic-demo",
                device_id=args.device_id,
                stream_id=args.stream_id,
                sequence_number=index + 1,
                previous_payload_hash=previous_payload_hash,
                start_frame=index,
                end_frame=index,
                frame_count=1,
            )
            record = create_signed_record(payload, private_key)
            manifest.write(json.dumps(record, sort_keys=True) + "\n")
            if not args.no_submit:
                post_record(args.ingest_url, record)
            previous_payload_hash = record["payloadHash"]

            if index == 0:
                cv2.imwrite(str(artifact_dir / f"{args.stream_id}-redacted.jpg"), redacted)

    print(json.dumps({
        "status": "created",
        "streamId": args.stream_id,
        "records": args.records,
        "submitted": not args.no_submit,
        "manifest": str(manifest_path.resolve()),
    }, indent=2))


if __name__ == "__main__":
    main()
