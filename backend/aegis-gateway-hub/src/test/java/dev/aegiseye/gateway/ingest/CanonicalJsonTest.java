package dev.aegiseye.gateway.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalJsonTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CanonicalJson canonicalJson = new CanonicalJson(objectMapper);

    @Test
    void canonicalHashIsIndependentOfObjectFieldOrder() throws Exception {
        JsonNode first = objectMapper.readTree("""
                {"b":2,"a":{"d":4,"c":3}}
                """);
        JsonNode second = objectMapper.readTree("""
                {"a":{"c":3,"d":4},"b":2}
                """);

        assertThat(canonicalJson.canonicalString(first)).isEqualTo(canonicalJson.canonicalString(second));
        assertThat(canonicalJson.sha256Hex(canonicalJson.canonicalBytes(first)))
                .isEqualTo(canonicalJson.sha256Hex(canonicalJson.canonicalBytes(second)));
    }
}
