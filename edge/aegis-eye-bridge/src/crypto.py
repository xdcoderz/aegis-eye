import base64

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey, Ed25519PublicKey
from cryptography.hazmat.primitives.serialization import (
    Encoding,
    NoEncryption,
    PrivateFormat,
    PublicFormat,
)


def generate_demo_private_key() -> Ed25519PrivateKey:
    return Ed25519PrivateKey.generate()


def public_key_hex(public_key: Ed25519PublicKey) -> str:
    return public_key.public_bytes(
        encoding=Encoding.Raw,
        format=PublicFormat.Raw,
    ).hex()


def private_key_pem(private_key: Ed25519PrivateKey) -> bytes:
    return private_key.private_bytes(
        encoding=Encoding.PEM,
        format=PrivateFormat.PKCS8,
        encryption_algorithm=NoEncryption(),
    )


def sign_hash(private_key: Ed25519PrivateKey, payload_hash_hex: str) -> str:
    signature = private_key.sign(payload_hash_hex.encode("ascii"))
    return base64.b64encode(signature).decode("ascii")


def verify_hash_signature(public_key: Ed25519PublicKey, payload_hash_hex: str, signature_b64: str) -> bool:
    try:
        public_key.verify(
            base64.b64decode(signature_b64.encode("ascii")),
            payload_hash_hex.encode("ascii"),
        )
        return True
    except InvalidSignature:
        return False
