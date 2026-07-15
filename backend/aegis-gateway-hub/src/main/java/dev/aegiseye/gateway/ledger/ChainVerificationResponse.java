package dev.aegiseye.gateway.ledger;

import java.time.Instant;

public record ChainVerificationResponse(
        String deviceId,
        String streamId,
        String status,
        int checkedRecords,
        int totalRecords,
        Long firstBrokenSequenceNumber,
        String reason,
        Instant verifiedAt
) {
}
