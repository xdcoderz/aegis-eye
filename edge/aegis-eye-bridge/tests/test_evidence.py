import unittest

from src.canonical import payload_hash
from src.crypto import generate_demo_private_key
from src.evidence import build_payload, create_signed_record


class EvidenceRecordTests(unittest.TestCase):
    def test_records_form_a_hash_chain(self) -> None:
        private_key = generate_demo_private_key()
        first_payload = build_payload(
            redacted_frame_hash="a" * 64,
            detections=[],
            model_name="test-redactor",
            model_version="1.0",
            model_mode="opencv-haar",
            device_id="edge-demo-001",
            stream_id="demo-test",
            sequence_number=1,
            previous_payload_hash=None,
            start_frame=0,
            end_frame=0,
            frame_count=1,
        )
        first_record = create_signed_record(first_payload, private_key)
        second_payload = build_payload(
            redacted_frame_hash="b" * 64,
            detections=[],
            model_name="test-redactor",
            model_version="1.0",
            model_mode="opencv-haar",
            device_id="edge-demo-001",
            stream_id="demo-test",
            sequence_number=2,
            previous_payload_hash=first_record["payloadHash"],
            start_frame=1,
            end_frame=1,
            frame_count=1,
        )
        second_record = create_signed_record(second_payload, private_key)

        self.assertEqual(first_record["payloadHash"], payload_hash(first_payload))
        self.assertEqual(second_record["previousPayloadHash"], first_record["payloadHash"])
        self.assertNotEqual(second_record["payloadHash"], first_record["payloadHash"])


if __name__ == "__main__":
    unittest.main()
