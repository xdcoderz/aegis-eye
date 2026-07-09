import unittest

from src.crypto import generate_demo_private_key, sign_hash, verify_hash_signature


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


if __name__ == "__main__":
    unittest.main()
