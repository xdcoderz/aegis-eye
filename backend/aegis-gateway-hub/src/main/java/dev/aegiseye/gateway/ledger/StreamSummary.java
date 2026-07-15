package dev.aegiseye.gateway.ledger;

import java.time.Instant;

public record StreamSummary(
        String deviceId,
        String streamId,
        long recordCount,
        long firstSequenceNumber,
        long lastSequenceNumber,
        Instant latestReceivedAt,
        String integrityStatus
) {
}
