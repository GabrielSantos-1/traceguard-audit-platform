package com.traceguard.common.sanitization;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;

@Component
public class InputSanitizer {

    public String sanitizeText(String input, int maxLength, String fieldName) {
        if (input == null) {
            return null;
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKC)
                .replace("\r", " ")
                .replace("\n", " ")
                .replace("\t", " ")
                .replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ")
                .trim();

        if (normalized.length() > maxLength) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " length exceeds " + maxLength
            );
        }

        return normalized;
    }

    public String sanitizeJsonPayload(String input, int maxLength, String fieldName) {
        if (input == null) {
            return null;
        }

        String normalized = input.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.length() > maxLength) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " length exceeds " + maxLength
            );
        }

        return normalized;
    }
}
