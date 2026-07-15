package dev.aegiseye.gateway.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class EvidenceIngestionService {
    private static final Pattern SHA_256_HEX = Pattern.compile("^[a-f0-9]{64}$");

    private final JdbcTemplate jdbcTemplate;
    private final CanonicalJson canonicalJson;
    private final Ed25519Verifier verifier;

    public EvidenceIngestionService(JdbcTemplate jdbcTemplate, CanonicalJson canonicalJson, Ed25519Verifier verifier) {
        this.jdbcTemplate = jdbcTemplate;
        this.canonicalJson = canonicalJson;
        this.verifier = verifier;
    }

    @Transactional
    public EvidenceIngestionResponse ingest(EvidenceIngestionRequest request) {
        validateRequestShape(request);

        byte[] canonicalBytes = canonicalJson.canonicalBytes(request.canonicalPayload());
        String recomputedHash = canonicalJson.sha256Hex(canonicalBytes);
        if (!recomputedHash.equals(request.payloadHash())) {
            throw new InvalidEvidenceException("payloadHash does not match canonicalPayload");
        }

        String payloadPreviousHash = nullableText(request.canonicalPayload().path("previousPayloadHash"));
        if (!safeEquals(payloadPreviousHash, request.previousPayloadHash())) {
            throw new InvalidEvidenceException("previousPayloadHash does not match canonicalPayload");
        }

        if (!"ed25519".equalsIgnoreCase(request.signatureAlgorithm())) {
            throw new InvalidEvidenceException("Only ed25519 signatures are supported in the MVP");
        }

        boolean signatureValid = verifier.verify(request.publicKeyHex(), request.payloadHash(), request.signature());
        if (!signatureValid) {
            throw new InvalidEvidenceException("signature verification failed");
        }

        JsonNode payload = request.canonicalPayload();
        String deviceId = requiredText(payload, "deviceId");
        String streamId = requiredText(payload, "streamId");
        String schemaVersion = requiredText(payload, "schemaVersion");
        long sequenceNumber = requiredLong(payload, "sequenceNumber");
        Instant capturedAt = parseInstant(requiredText(payload, "capturedAt"));

        pairOrVerifyDeviceKey(deviceId, request.publicKeyId(), request.publicKeyHex());
        verifyChainPosition(deviceId, streamId, sequenceNumber, request.previousPayloadHash());

        UUID ledgerId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO evidence_ledger (
                    id,
                    device_id,
                    stream_id,
                    sequence_number,
                    schema_version,
                    captured_at,
                    payload_hash,
                    previous_payload_hash,
                    signature,
                    signature_algorithm,
                    public_key_id,
                    canonical_payload,
                    verification_status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, 'valid')
                """,
                ledgerId,
                deviceId,
                streamId,
                sequenceNumber,
                schemaVersion,
                Timestamp.from(capturedAt),
                request.payloadHash(),
                request.previousPayloadHash(),
                request.signature(),
                request.signatureAlgorithm().toLowerCase(),
                request.publicKeyId(),
                canonicalJson.canonicalString(payload)
        );

        return new EvidenceIngestionResponse(
                ledgerId,
                deviceId,
                streamId,
                sequenceNumber,
                request.payloadHash(),
                "valid"
        );
    }

    private void validateRequestShape(EvidenceIngestionRequest request) {
        if (request == null) {
            throw new InvalidEvidenceException("request body is required");
        }
        requireSha256("payloadHash", request.payloadHash());
        if (request.previousPayloadHash() != null) {
            requireSha256("previousPayloadHash", request.previousPayloadHash());
        }
        requireText("signature", request.signature());
        requireText("signatureAlgorithm", request.signatureAlgorithm());
        requireText("publicKeyId", request.publicKeyId());
        requireText("publicKeyHex", request.publicKeyHex());
        if (request.canonicalPayload() == null || !request.canonicalPayload().isObject()) {
            throw new InvalidEvidenceException("canonicalPayload must be an object");
        }
    }

    private void requireSha256(String field, String value) {
        requireText(field, value);
        if (!SHA_256_HEX.matcher(value).matches()) {
            throw new InvalidEvidenceException(field + " must be a lowercase SHA-256 hex string");
        }
    }

    private void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidEvidenceException(field + " is required");
        }
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new InvalidEvidenceException("canonicalPayload." + field + " is required");
        }
        return value.asText();
    }

    private long requiredLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.canConvertToLong()) {
            throw new InvalidEvidenceException("canonicalPayload." + field + " must be an integer");
        }
        return value.asLong();
    }

    private Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (Exception exception) {
            throw new InvalidEvidenceException("canonicalPayload.capturedAt must be an ISO-8601 instant", exception);
        }
    }

    private String nullableText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw new InvalidEvidenceException("canonicalPayload.previousPayloadHash must be null or text");
        }
        return node.asText();
    }

    private boolean safeEquals(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private void pairOrVerifyDeviceKey(String deviceId, String publicKeyId, String publicKeyHex) {
        if (!publicKeyHex.matches("^[a-f0-9]{64}$")) {
            throw new InvalidEvidenceException("publicKeyHex must be a lowercase 32-byte Ed25519 key");
        }

        var rows = jdbcTemplate.query(
                """
                SELECT public_key_hex
                FROM edge_device
                WHERE device_id = ?
                  AND public_key_id = ?
                  AND status = 'active'
                """,
                (resultSet, rowNumber) -> resultSet.getString("public_key_hex"),
                deviceId,
                publicKeyId
        );
        if (rows.isEmpty()) {
            throw new InvalidEvidenceException("device is not registered as active for this publicKeyId");
        }

        String registeredKey = rows.getFirst();
        if (registeredKey == null || registeredKey.isBlank()) {
            jdbcTemplate.update(
                    """
                    UPDATE edge_device
                    SET public_key_hex = ?, paired_at = now(), updated_at = now()
                    WHERE device_id = ? AND public_key_id = ? AND public_key_hex IS NULL
                    """,
                    publicKeyHex,
                    deviceId,
                    publicKeyId
            );
            registeredKey = jdbcTemplate.queryForObject(
                    "SELECT public_key_hex FROM edge_device WHERE device_id = ? AND public_key_id = ?",
                    String.class,
                    deviceId,
                    publicKeyId
            );
        }

        if (registeredKey == null || !registeredKey.trim().equals(publicKeyHex)) {
            throw new InvalidEvidenceException("public key does not match the key paired to this device");
        }
    }

    private void verifyChainPosition(
            String deviceId,
            String streamId,
            long sequenceNumber,
            String previousPayloadHash
    ) {
        var latest = jdbcTemplate.query(
                """
                SELECT sequence_number, payload_hash
                FROM evidence_ledger
                WHERE device_id = ? AND stream_id = ?
                ORDER BY sequence_number DESC
                LIMIT 1
                FOR UPDATE
                """,
                (resultSet, rowNumber) -> new ChainHead(
                        resultSet.getLong("sequence_number"),
                        resultSet.getString("payload_hash")
                ),
                deviceId,
                streamId
        );

        if (latest.isEmpty()) {
            if (sequenceNumber != 1 || previousPayloadHash != null) {
                throw new InvalidEvidenceException("a new stream must start at sequenceNumber 1 with no previousPayloadHash");
            }
            return;
        }

        ChainHead head = latest.getFirst();
        if (sequenceNumber != head.sequenceNumber() + 1) {
            throw new InvalidEvidenceException("sequenceNumber must continue from the latest accepted record");
        }
        if (!head.payloadHash().equals(previousPayloadHash)) {
            throw new InvalidEvidenceException("previousPayloadHash must match the latest accepted record");
        }
    }

    private record ChainHead(long sequenceNumber, String payloadHash) {
    }
}
