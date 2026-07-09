package dev.aegiseye.gateway.ingest;

import com.fasterxml.jackson.databind.JsonNode;

public record EvidenceIngestionRequest(
        String payloadHash,
        String previousPayloadHash,
        String signature,
        String signatureAlgorithm,
        String publicKeyId,
        String publicKeyHex,
        JsonNode canonicalPayload
) {
}
