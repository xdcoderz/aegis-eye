package dev.aegiseye.gateway.ingest;

import java.util.UUID;

public record EvidenceIngestionResponse(
        UUID ledgerId,
        String deviceId,
        String streamId,
        long sequenceNumber,
        String payloadHash,
        String verificationStatus
) {
}
