import unittest

from src.canonical import canonical_json_bytes, payload_hash


class CanonicalJsonTests(unittest.TestCase):
    def test_payload_hash_is_independent_of_dict_insertion_order(self) -> None:
        first = {"b": 2, "a": {"d": 4, "c": 3}}
        second = {"a": {"c": 3, "d": 4}, "b": 2}

        self.assertEqual(canonical_json_bytes(first), canonical_json_bytes(second))
        self.assertEqual(payload_hash(first), payload_hash(second))


if __name__ == "__main__":
    unittest.main()
