package dev.aegiseye.gateway.ledger;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

record StoredEvidence(
        UUID id,
        String deviceId,
        String streamId,
        long sequenceNumber,
        Instant capturedAt,
        Instant receivedAt,
        String payloadHash,
        String previousPayloadHash,
        String signature,
        String signatureAlgorithm,
        String publicKeyId,
        String publicKeyHex,
        JsonNode canonicalPayload,
        String verificationStatus,
        String verificationError
) {
}
