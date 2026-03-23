package com.food_delivery.backend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum Role {
    USER,
    ADMIN,
    RESTAURANT_OWNER,
    DELIVERY_AGENT;

    @JsonCreator
    public static Role fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);

        if ("DELIVERY_BOY".equals(normalized)) {
            return DELIVERY_AGENT;
        }

        try {
            return Role.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            String allowed = Arrays.stream(Role.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Invalid role. Allowed roles: " + allowed);
        }
    }
}