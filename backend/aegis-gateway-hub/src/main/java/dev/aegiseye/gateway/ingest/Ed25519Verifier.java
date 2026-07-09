package dev.aegiseye.gateway.ingest;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class Ed25519Verifier {
    private static final byte[] ED25519_SPKI_PREFIX = HexFormat.of().parseHex("302a300506032b6570032100");

    public boolean verify(String publicKeyHex, String payloadHash, String signatureBase64) {
        try {
            byte[] rawPublicKey = HexFormat.of().parseHex(publicKeyHex);
            byte[] spki = new byte[ED25519_SPKI_PREFIX.length + rawPublicKey.length];
            System.arraycopy(ED25519_SPKI_PREFIX, 0, spki, 0, ED25519_SPKI_PREFIX.length);
            System.arraycopy(rawPublicKey, 0, spki, ED25519_SPKI_PREFIX.length, rawPublicKey.length);

            PublicKey publicKey = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(spki));
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(payloadHash.getBytes(StandardCharsets.US_ASCII));
            return signature.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception exception) {
            return false;
        }
    }
}
