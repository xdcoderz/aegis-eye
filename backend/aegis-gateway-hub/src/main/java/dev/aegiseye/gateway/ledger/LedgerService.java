package dev.aegiseye.gateway.ledger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aegiseye.gateway.ingest.CanonicalJson;
import dev.aegiseye.gateway.ingest.Ed25519Verifier;
import dev.aegiseye.gateway.ingest.InvalidEvidenceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@Service
public class LedgerService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final CanonicalJson canonicalJson;
    private final Ed25519Verifier signatureVerifier;

    public LedgerService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            CanonicalJson canonicalJson,
            Ed25519Verifier signatureVerifier
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.canonicalJson = canonicalJson;
        this.signatureVerifier = signatureVerifier;
    }

    public List<StreamSummary> listStreams() {
        return jdbcTemplate.query(
                """
                SELECT device_id,
                       stream_id,
                       count(*) AS record_count,
                       min(sequence_number) AS first_sequence,
                       max(sequence_number) AS last_sequence,
                       max(received_at) AS latest_received_at,
                       CASE
                           WHEN bool_or(verification_status = 'invalid') THEN 'broken'
                           WHEN bool_and(verification_status = 'valid') THEN 'valid'
                           ELSE 'pending'
                       END AS integrity_status
                FROM evidence_ledger
                GROUP BY device_id, stream_id
                ORDER BY max(received_at) DESC
                """,
                (resultSet, rowNumber) -> new StreamSummary(
                        resultSet.getString("device_id"),
                        resultSet.getString("stream_id"),
                        resultSet.getLong("record_count"),
                        resultSet.getLong("first_sequence"),
                        resultSet.getLong("last_sequence"),
                        resultSet.getTimestamp("latest_received_at").toInstant(),
                        resultSet.getString("integrity_status")
                )
        );
    }

    public List<LedgerRecordResponse> listRecords(String deviceId, String streamId) {
        requireIdentity(deviceId, streamId);
        return loadRecords(deviceId, streamId).stream()
                .map(record -> new LedgerRecordResponse(
                        record.id(),
                        record.sequenceNumber(),
                        record.capturedAt(),
                        record.receivedAt(),
                        record.payloadHash(),
                        record.previousPayloadHash(),
                        record.canonicalPayload().path("detections").size(),
                        record.canonicalPayload().path("redactionModel").path("name").asText("unknown"),
                        record.verificationStatus(),
                        record.verificationError()
                ))
                .toList();
    }

    @Transactional
    public ChainVerificationResponse verify(ChainVerificationRequest request) {
        if (request == null) {
            throw new InvalidEvidenceException("request body is required");
        }
        requireIdentity(request.deviceId(), request.streamId());

        List<StoredEvidence> records = loadRecords(request.deviceId(), request.streamId());
        Instant verifiedAt = Instant.now();
        if (records.isEmpty()) {
            return new ChainVerificationResponse(
                    request.deviceId(), request.streamId(), "empty", 0, 0, null,
                    "No evidence records exist for this stream", verifiedAt
            );
        }

        jdbcTemplate.update(
                """
                UPDATE evidence_ledger
                SET verification_status = 'pending', verification_error = NULL
                WHERE device_id = ? AND stream_id = ?
                """,
                request.deviceId(),
                request.streamId()
        );

        String expectedPreviousHash = null;
        long expectedSequence = 1;
        int checked = 0;
        for (StoredEvidence record : records) {
            String failure = verifyRecord(record, expectedSequence, expectedPreviousHash);
            checked++;
            if (failure != null) {
                jdbcTemplate.update(
                        """
                        UPDATE evidence_ledger
                        SET verification_status = 'invalid', verification_error = ?
                        WHERE id = ?
                        """,
                        failure,
                        record.id()
                );
                long brokenSequence = record.sequenceNumber() == expectedSequence
                        ? record.sequenceNumber()
                        : expectedSequence;
                return new ChainVerificationResponse(
                        request.deviceId(), request.streamId(), "broken", checked, records.size(),
                        brokenSequence, failure, verifiedAt
                );
            }

            jdbcTemplate.update(
                    """
                    UPDATE evidence_ledger
                    SET verification_status = 'valid', verification_error = NULL
                    WHERE id = ?
                    """,
                    record.id()
            );
            expectedPreviousHash = record.payloadHash();
            expectedSequence++;
        }

        return new ChainVerificationResponse(
                request.deviceId(), request.streamId(), "valid", checked, records.size(), null,
                "Every stored payload, signature, sequence, and chain link is valid", verifiedAt
        );
    }

    private String verifyRecord(StoredEvidence record, long expectedSequence, String expectedPreviousHash) {
        if (record.sequenceNumber() != expectedSequence) {
            return "Missing or out-of-order record: expected sequence " + expectedSequence
                    + " but found " + record.sequenceNumber();
        }

        JsonNode payload = record.canonicalPayload();
        if (!record.deviceId().equals(payload.path("deviceId").asText())
                || !record.streamId().equals(payload.path("streamId").asText())
                || record.sequenceNumber() != payload.path("sequenceNumber").asLong(-1)) {
            return "Stored row metadata does not match its canonical payload";
        }

        String recomputedHash = canonicalJson.sha256Hex(canonicalJson.canonicalBytes(payload));
        if (!record.payloadHash().equals(recomputedHash)) {
            return "Canonical payload hash mismatch";
        }

        String payloadPreviousHash = payload.path("previousPayloadHash").isNull()
                ? null
                : payload.path("previousPayloadHash").asText();
        if (!safeEquals(record.previousPayloadHash(), payloadPreviousHash)) {
            return "Stored previous hash does not match the canonical payload";
        }
        if (!safeEquals(expectedPreviousHash, record.previousPayloadHash())) {
            return "Broken chain link to the previous evidence record";
        }

        if (!"ed25519".equalsIgnoreCase(record.signatureAlgorithm())) {
            return "Unsupported signature algorithm: " + record.signatureAlgorithm();
        }
        if (record.publicKeyHex() == null || record.publicKeyHex().isBlank()) {
            return "No registered public key is available for this device";
        }
        if (!signatureVerifier.verify(
                record.publicKeyHex().trim(),
                record.payloadHash(),
                record.signature()
        )) {
            return "Ed25519 signature verification failed";
        }
        return null;
    }

    private List<StoredEvidence> loadRecords(String deviceId, String streamId) {
        return jdbcTemplate.query(
                """
                SELECT l.id,
                       l.device_id,
                       l.stream_id,
                       l.sequence_number,
                       l.captured_at,
                       l.received_at,
                       l.payload_hash,
                       l.previous_payload_hash,
                       l.signature,
                       l.signature_algorithm,
                       l.public_key_id,
                       d.public_key_hex,
                       l.canonical_payload::text AS canonical_payload,
                       l.verification_status,
                       l.verification_error
                FROM evidence_ledger l
                LEFT JOIN edge_device d
                  ON d.device_id = l.device_id AND d.public_key_id = l.public_key_id
                WHERE l.device_id = ? AND l.stream_id = ?
                ORDER BY l.sequence_number
                """,
                this::mapStoredEvidence,
                deviceId,
                streamId
        );
    }

    private StoredEvidence mapStoredEvidence(ResultSet resultSet, int rowNumber) throws SQLException {
        try {
            return new StoredEvidence(
                    resultSet.getObject("id", java.util.UUID.class),
                    resultSet.getString("device_id"),
                    resultSet.getString("stream_id"),
                    resultSet.getLong("sequence_number"),
                    resultSet.getTimestamp("captured_at").toInstant(),
                    resultSet.getTimestamp("received_at").toInstant(),
                    resultSet.getString("payload_hash"),
                    resultSet.getString("previous_payload_hash"),
                    resultSet.getString("signature"),
                    resultSet.getString("signature_algorithm"),
                    resultSet.getString("public_key_id"),
                    resultSet.getString("public_key_hex"),
                    objectMapper.readTree(resultSet.getString("canonical_payload")),
                    resultSet.getString("verification_status"),
                    resultSet.getString("verification_error")
            );
        } catch (Exception exception) {
            throw new SQLException("Stored canonical payload is not valid JSON", exception);
        }
    }

    private void requireIdentity(String deviceId, String streamId) {
        if (deviceId == null || deviceId.isBlank() || streamId == null || streamId.isBlank()) {
            throw new InvalidEvidenceException("deviceId and streamId are required");
        }
    }

    private boolean safeEquals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
