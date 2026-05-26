package com.traceguard.adminaudit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditSanitizerTest {

    private AuditSanitizer sanitizer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sanitizer = new AuditSanitizer(objectMapper);
    }

    @Test
    void sanitizeForPersistenceRemovesProhibitedAndMasksSensitiveFields() {
        JsonNode node = sanitizer.sanitizeForPersistence(Map.of(
                "password", "pass",
                "accessToken", "jwt",
                "nested", Map.of("refreshToken", "rt-123", "authorization", "Bearer x"),
                "email", "admin@traceguard.local",
                "ip", "192.168.10.25",
                "userAgent", "Mozilla/5.0"));

        assertThat(node.has("password")).isFalse();
        assertThat(node.has("accessToken")).isFalse();
        assertThat(node.get("nested").has("refreshToken")).isFalse();
        assertThat(node.get("nested").has("authorization")).isFalse();
        assertThat(node.get("email").asText()).isEqualTo("a***@traceguard.local");
        assertThat(node.get("ip").asText()).isEqualTo("192.168.***.***");
        assertThat(node.get("userAgent").asText()).isEqualTo("Mozilla/5.0");
    }

    @Test
    void sanitizeForPersistenceTruncatesOversizedPayload() {
        Map<String, Object> details = new HashMap<>();
        for (int i = 0; i < 95; i++) {
            details.put("field_" + i, "x".repeat(80));
        }
        JsonNode node = sanitizer.sanitizeForPersistence(details);

        assertThat(node.get("truncated").asBoolean()).isTrue();
        assertThat(node.get("size").asInt()).isGreaterThan(5000);
    }

    @Test
    void sanitizeForResponseReinforcesPolicy() {
        JsonNode raw = objectMapper.valueToTree(Map.of(
                "token", "raw-token",
                "apiKey", "raw-key",
                "email", "admin@traceguard.local"));

        JsonNode node = sanitizer.sanitizeForResponse(raw);
        assertThat(node.has("token")).isFalse();
        assertThat(node.has("apiKey")).isFalse();
        assertThat(node.get("email").asText()).isEqualTo("a***@traceguard.local");
    }

    @Test
    void sanitizeForPersistenceHandlesUnexpectedStructureSafely() {
        JsonNode raw = objectMapper.getNodeFactory().arrayNode()
                .add(objectMapper.getNodeFactory().objectNode().put("secret", "x"));

        JsonNode node = sanitizer.sanitizeForResponse(raw);
        assertThat(node.isArray()).isTrue();
        assertThat(node.get(0).has("secret")).isFalse();
    }
}
