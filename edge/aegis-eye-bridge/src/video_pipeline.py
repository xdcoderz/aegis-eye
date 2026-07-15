from __future__ import annotations

import argparse
import json
from pathlib import Path

import cv2
import numpy as np

from .canonical import sha256_hex
from .crypto import load_or_create_private_key
from .evidence import build_payload, create_signed_record, post_record


def parse_source(value: str) -> int | str:
    return int(value) if value.isdigit() else value


def frame_hash(frame: np.ndarray) -> str:
    ok, encoded = cv2.imencode(".jpg", frame, [int(cv2.IMWRITE_JPEG_QUALITY), 90])
    if not ok:
        raise RuntimeError("failed to encode redacted frame")
    return sha256_hex(encoded.tobytes())


def redact_faces(frame: np.ndarray, detector: cv2.CascadeClassifier) -> tuple[np.ndarray, list[dict]]:
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    faces = detector.detectMultiScale(
        gray,
        scaleFactor=1.1,
        minNeighbors=5,
        minSize=(40, 40),
    )
    redacted = frame.copy()
    detections: list[dict] = []
    for x, y, width, height in faces:
        roi = redacted[y : y + height, x : x + width]
        kernel = min(99, max(31, (min(width, height) // 3) | 1))
        redacted[y : y + height, x : x + width] = cv2.GaussianBlur(roi, (kernel, kernel), 0)
        detections.append(
            {
                "type": "face",
                "confidence": 0.85,
                "redaction": "blur",
                "box": {
                    "x": int(x),
                    "y": int(y),
                    "width": int(width),
                    "height": int(height),
                },
            }
        )
    return redacted, detections


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Redact faces and emit signed AegisEye evidence")
    parser.add_argument("--source", default="0", help="webcam index or input video path")
    parser.add_argument("--output", default="artifacts/redacted-output.mp4")
    parser.add_argument("--manifest", default="artifacts/evidence-records.jsonl")
    parser.add_argument("--key", default="keys/demo-device-ed25519.pem")
    parser.add_argument("--device-id", default="edge-demo-001")
    parser.add_argument("--stream-id", default="demo-webcam")
    parser.add_argument("--ingest-url", default="http://localhost:8080/api/evidence")
    parser.add_argument("--evidence-every", type=int, default=30, help="emit evidence every N frames")
    parser.add_argument("--max-frames", type=int, default=180, help="0 processes until source ends")
    parser.add_argument("--display", action="store_true", help="show the redacted preview; Esc stops")
    parser.add_argument("--no-submit", action="store_true", help="create local evidence without HTTP submission")
    return parser


def run(args: argparse.Namespace) -> dict[str, int | str]:
    if args.evidence_every < 1:
        raise ValueError("--evidence-every must be at least 1")

    cascade_path = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
    detector = cv2.CascadeClassifier(cascade_path)
    if detector.empty():
        raise RuntimeError(f"could not load OpenCV face detector from {cascade_path}")

    capture = cv2.VideoCapture(parse_source(args.source))
    if not capture.isOpened():
        raise RuntimeError(f"could not open video source {args.source}")

    output_path = Path(args.output)
    manifest_path = Path(args.manifest)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.parent.mkdir(parents=True, exist_ok=True)

    width = int(capture.get(cv2.CAP_PROP_FRAME_WIDTH)) or 640
    height = int(capture.get(cv2.CAP_PROP_FRAME_HEIGHT)) or 480
    fps = capture.get(cv2.CAP_PROP_FPS)
    fps = fps if fps and fps > 1 else 20.0
    writer = cv2.VideoWriter(
        str(output_path),
        cv2.VideoWriter_fourcc(*"mp4v"),
        fps,
        (width, height),
    )
    if not writer.isOpened():
        capture.release()
        raise RuntimeError(f"could not open output video {output_path}")

    private_key = load_or_create_private_key(Path(args.key))
    frame_index = 0
    sequence_number = 1
    previous_payload_hash: str | None = None
    detection_total = 0

    try:
        with manifest_path.open("w", encoding="utf-8") as manifest:
            while args.max_frames == 0 or frame_index < args.max_frames:
                ok, frame = capture.read()
                if not ok:
                    break

                redacted, detections = redact_faces(frame, detector)
                detection_total += len(detections)
                writer.write(redacted)

                if frame_index % args.evidence_every == 0:
                    payload = build_payload(
                        redacted_frame_hash=frame_hash(redacted),
                        detections=detections,
                        model_name="opencv-haar-face-redactor",
                        model_version=cv2.__version__,
                        model_mode="opencv-haar",
                        device_id=args.device_id,
                        stream_id=args.stream_id,
                        sequence_number=sequence_number,
                        previous_payload_hash=previous_payload_hash,
                        start_frame=frame_index,
                        end_frame=frame_index,
                        frame_count=1,
                    )
                    record = create_signed_record(payload, private_key)
                    manifest.write(json.dumps(record, sort_keys=True) + "\n")
                    manifest.flush()
                    if not args.no_submit:
                        post_record(args.ingest_url, record)
                    previous_payload_hash = record["payloadHash"]
                    sequence_number += 1

                frame_index += 1
                if args.display:
                    cv2.imshow("AegisEye redacted output", redacted)
                    if cv2.waitKey(1) & 0xFF == 27:
                        break
    finally:
        capture.release()
        writer.release()
        cv2.destroyAllWindows()

    result = {
        "framesProcessed": frame_index,
        "evidenceRecords": sequence_number - 1,
        "facesRedacted": detection_total,
        "streamId": args.stream_id,
        "output": str(output_path.resolve()),
        "manifest": str(manifest_path.resolve()),
    }
    print(json.dumps(result, indent=2))
    return result


def main() -> None:
    run(build_parser().parse_args())


if __name__ == "__main__":
    main()
