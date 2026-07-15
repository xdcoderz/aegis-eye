import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from src.crypto import generate_demo_private_key, load_or_create_private_key, sign_hash, verify_hash_signature


class SignatureTests(unittest.TestCase):
    def test_signature_verifies_for_original_hash(self) -> None:
        private_key = generate_demo_private_key()
        payload_hash = "a" * 64
        signature = sign_hash(private_key, payload_hash)

        self.assertTrue(verify_hash_signature(private_key.public_key(), payload_hash, signature))

    def test_signature_rejects_tampered_hash(self) -> None:
        private_key = generate_demo_private_key()
        signature = sign_hash(private_key, "a" * 64)

        self.assertFalse(verify_hash_signature(private_key.public_key(), "b" * 64, signature))

    def test_device_key_is_stable_across_runs(self) -> None:
        with TemporaryDirectory() as directory:
            path = Path(directory) / "device.pem"
            first = load_or_create_private_key(path)
            second = load_or_create_private_key(path)

            signature = sign_hash(first, "c" * 64)
            self.assertTrue(verify_hash_signature(second.public_key(), "c" * 64, signature))


if __name__ == "__main__":
    unittest.main()
