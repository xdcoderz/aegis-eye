package dev.aegiseye.gateway.ledger;

import java.time.Instant;
import java.util.UUID;

public record LedgerRecordResponse(
        UUID id,
        long sequenceNumber,
        Instant capturedAt,
        Instant receivedAt,
        String payloadHash,
        String previousPayloadHash,
        int detectionCount,
        String redactionModel,
        String verificationStatus,
        String verificationError
) {
}
