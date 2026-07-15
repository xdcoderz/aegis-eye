import unittest

import numpy as np

from src.video_pipeline import frame_hash, parse_source, redact_faces


class FakeDetector:
    def detectMultiScale(self, *_args, **_kwargs):
        return np.array([[20, 20, 60, 60]])


class VideoPipelineTests(unittest.TestCase):
    def test_source_parsing_distinguishes_camera_and_file(self) -> None:
        self.assertEqual(parse_source("0"), 0)
        self.assertEqual(parse_source("sample.mp4"), "sample.mp4")

    def test_detected_face_region_is_redacted_and_hashed(self) -> None:
        frame = np.zeros((120, 120, 3), dtype=np.uint8)
        checker = np.indices((60, 60)).sum(axis=0) % 2
        frame[20:80, 20:80] = (checker[:, :, None] * 255).astype(np.uint8)

        redacted, detections = redact_faces(frame, FakeDetector())

        self.assertEqual(len(detections), 1)
        self.assertFalse(np.array_equal(frame[20:80, 20:80], redacted[20:80, 20:80]))
        self.assertEqual(len(frame_hash(redacted)), 64)


if __name__ == "__main__":
    unittest.main()
