package dev.aegiseye.gateway.ingest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class CanonicalJson {
    private final ObjectMapper objectMapper;

    public CanonicalJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] canonicalBytes(JsonNode node) {
        try {
            return objectMapper.writeValueAsBytes(normalize(node));
        } catch (JsonProcessingException exception) {
            throw new InvalidEvidenceException("Canonical payload could not be serialized", exception);
        }
    }

    public String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private Object normalize(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            Map<String, Object> normalized = new TreeMap<>();
            node.fields().forEachRemaining(entry -> normalized.put(entry.getKey(), normalize(entry.getValue())));
            return normalized;
        }
        if (node.isArray()) {
            List<Object> normalized = new ArrayList<>();
            node.forEach(value -> normalized.add(normalize(value)));
            return normalized;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isIntegralNumber()) {
            return node.longValue();
        }
        if (node.isFloatingPointNumber() || node.isBigDecimal()) {
            return new BigDecimal(node.asText());
        }
        return node.asText();
    }

    public String canonicalString(JsonNode node) {
        return new String(canonicalBytes(node), StandardCharsets.UTF_8);
    }
}
