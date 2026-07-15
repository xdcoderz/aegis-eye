import base64
from pathlib import Path

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey, Ed25519PublicKey
from cryptography.hazmat.primitives.serialization import (
    Encoding,
    NoEncryption,
    PrivateFormat,
    PublicFormat,
    load_pem_private_key,
)


def generate_demo_private_key() -> Ed25519PrivateKey:
    return Ed25519PrivateKey.generate()


def load_or_create_private_key(path: Path) -> Ed25519PrivateKey:
    if path.exists():
        loaded_key = load_pem_private_key(path.read_bytes(), password=None)
        if not isinstance(loaded_key, Ed25519PrivateKey):
            raise ValueError(f"{path} does not contain an Ed25519 private key")
        return loaded_key

    path.parent.mkdir(parents=True, exist_ok=True)
    private_key = generate_demo_private_key()
    path.write_bytes(private_key_pem(private_key))
    path.chmod(0o600)
    return private_key


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
