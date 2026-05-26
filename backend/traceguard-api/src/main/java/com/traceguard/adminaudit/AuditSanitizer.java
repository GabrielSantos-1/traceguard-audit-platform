package com.traceguard.adminaudit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class AuditSanitizer {

    private static final Logger log = LoggerFactory.getLogger(AuditSanitizer.class);

    private static final Set<String> PROHIBITED_KEY_PARTS = Set.of(
            "password", "senha", "token", "accesstoken", "refreshtoken", "apikey",
            "authorization", "cookie", "secret", "credential", "privatekey");
    private static final Set<String> MASKED_KEY_PARTS = Set.of("email", "ip", "useragent");

    private static final int MAX_JSON_CHARS = 5000;
    private static final int MAX_DEPTH = 8;
    private static final int MAX_FIELDS_PER_OBJECT = 100;
    private static final int MAX_ITEMS_PER_ARRAY = 100;
    private static final int MAX_STRING_LENGTH = 256;
    private static final int MAX_USER_AGENT_LENGTH = 180;

    private final ObjectMapper objectMapper;

    public AuditSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode sanitizeForPersistence(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.valueToTree(details);
            JsonNode sanitized = sanitizeNode(root, 0);
            return enforceSizeLimit(sanitized);
        } catch (Exception ex) {
            log.warn("event=admin_audit_sanitization_failed stage=persistence", ex);
            return fallbackNode();
        }
    }

    public JsonNode sanitizeForResponse(JsonNode details) {
        if (details == null || details.isNull()) {
            return null;
        }
        try {
            JsonNode sanitized = sanitizeNode(details, 0);
            return enforceSizeLimit(sanitized);
        } catch (Exception ex) {
            log.warn("event=admin_audit_sanitization_failed stage=response", ex);
            return fallbackNode();
        }
    }

    private JsonNode sanitizeNode(JsonNode node, int depth) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (depth > MAX_DEPTH) {
            return JsonNodeFactory.instance.textNode("[TRUNCATED_DEPTH]");
        }
        if (node.isObject()) {
            ObjectNode source = (ObjectNode) node;
            ObjectNode target = JsonNodeFactory.instance.objectNode();
            int processed = 0;
            var fields = source.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                if (processed >= MAX_FIELDS_PER_OBJECT) {
                    target.put("truncatedFields", true);
                    break;
                }

                String key = entry.getKey();
                String normalized = normalizeKey(key);
                if (isProhibited(normalized)) {
                    processed++;
                    continue;
                }

                JsonNode sanitizedValue = sanitizeValueByKey(normalized, entry.getValue(), depth + 1);
                if (sanitizedValue != null && !sanitizedValue.isMissingNode()) {
                    target.set(key, sanitizedValue);
                }
                processed++;
            }
            return target;
        }

        if (node.isArray()) {
            ArrayNode source = (ArrayNode) node;
            ArrayNode target = JsonNodeFactory.instance.arrayNode();
            int limit = Math.min(source.size(), MAX_ITEMS_PER_ARRAY);
            for (int i = 0; i < limit; i++) {
                JsonNode sanitized = sanitizeNode(source.get(i), depth + 1);
                target.add(sanitized == null ? JsonNodeFactory.instance.nullNode() : sanitized);
            }
            if (source.size() > MAX_ITEMS_PER_ARRAY) {
                target.add(JsonNodeFactory.instance.textNode("[TRUNCATED_ARRAY]"));
            }
            return target;
        }

        if (node.isTextual()) {
            return JsonNodeFactory.instance.textNode(truncate(node.asText(), MAX_STRING_LENGTH));
        }

        return node;
    }

    private JsonNode sanitizeValueByKey(String normalizedKey, JsonNode value, int depth) {
        if (isMasked(normalizedKey)) {
            return maskSensitiveValue(normalizedKey, value);
        }
        return sanitizeNode(value, depth);
    }

    private JsonNode maskSensitiveValue(String normalizedKey, JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        String raw = value.isTextual() ? value.asText() : value.toString();
        if (normalizedKey.contains("email")) {
            return JsonNodeFactory.instance.textNode(maskEmail(raw));
        }
        if (normalizedKey.contains("ip")) {
            return JsonNodeFactory.instance.textNode(maskIp(raw));
        }
        if (normalizedKey.contains("useragent")) {
            return JsonNodeFactory.instance.textNode(truncate(raw, MAX_USER_AGENT_LENGTH));
        }
        return JsonNodeFactory.instance.textNode("[MASKED]");
    }

    private JsonNode enforceSizeLimit(JsonNode node) {
        if (node == null) {
            return null;
        }
        String serialized = node.toString();
        if (serialized.length() <= MAX_JSON_CHARS) {
            return node;
        }
        ObjectNode truncated = JsonNodeFactory.instance.objectNode();
        truncated.put("truncated", true);
        truncated.put("size", serialized.length());
        return truncated;
    }

    private boolean isProhibited(String normalizedKey) {
        for (String part : PROHIBITED_KEY_PARTS) {
            if (normalizedKey.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMasked(String normalizedKey) {
        for (String part : MASKED_KEY_PARTS) {
            if (normalizedKey.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private String maskEmail(String value) {
        int at = value.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return value.charAt(0) + "***" + value.substring(at);
    }

    private String maskIp(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.contains(".")) {
            String[] parts = value.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".***.***";
            }
        }
        if (value.contains(":")) {
            String[] parts = value.split(":");
            if (parts.length >= 2) {
                return parts[0] + ":" + parts[1] + ":****";
            }
        }
        return "***";
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private JsonNode fallbackNode() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("sanitized", true);
        node.put("reason", "sanitization_failed");
        return node;
    }
}
