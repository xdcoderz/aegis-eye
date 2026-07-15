package dev.aegiseye.gateway.demo;

import dev.aegiseye.gateway.ingest.InvalidEvidenceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class DemoService {
    private final JdbcTemplate jdbcTemplate;
    private final boolean tamperEnabled;

    public DemoService(
            JdbcTemplate jdbcTemplate,
            @Value("${aegis.demo.tamper-enabled:false}") boolean tamperEnabled
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.tamperEnabled = tamperEnabled;
    }

    @Transactional
    public Map<String, Object> tamper(DemoTamperRequest request) {
        if (!tamperEnabled) {
            throw new InvalidEvidenceException("the intentional tamper endpoint is disabled");
        }
        if (request == null || request.deviceId() == null || request.streamId() == null) {
            throw new InvalidEvidenceException("deviceId, streamId, and sequenceNumber are required");
        }
        if (!request.streamId().startsWith("demo-")) {
            throw new InvalidEvidenceException("only streams prefixed with demo- can use intentional tampering");
        }
        if (request.sequenceNumber() < 1) {
            throw new InvalidEvidenceException("sequenceNumber must be at least 1");
        }

        int changed = jdbcTemplate.update(
                """
                UPDATE evidence_ledger
                SET canonical_payload = jsonb_set(
                        canonical_payload,
                        '{redactionModel,version}',
                        to_jsonb('tampered-by-hackathon-demo'::text),
                        true
                    ),
                    verification_status = 'pending',
                    verification_error = NULL
                WHERE device_id = ? AND stream_id = ? AND sequence_number = ?
                """,
                request.deviceId(),
                request.streamId(),
                request.sequenceNumber()
        );
        if (changed == 0) {
            throw new InvalidEvidenceException("the selected demo evidence record does not exist");
        }

        return Map.of(
                "status", "tampered",
                "streamId", request.streamId(),
                "sequenceNumber", request.sequenceNumber(),
                "message", "Canonical payload changed without updating its signed hash"
        );
    }
}
