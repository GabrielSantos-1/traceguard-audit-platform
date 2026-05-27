package com.traceguard.monitoring.activity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserActivityFilterTest {

    @Test
    void determineActionMapsHttpMethods() {
        assertThat(UserActivityFilter.determineAction("GET", 200)).isEqualTo("READ");
        assertThat(UserActivityFilter.determineAction("POST", 201)).isEqualTo("CREATE");
        assertThat(UserActivityFilter.determineAction("PUT", 200)).isEqualTo("UPDATE");
        assertThat(UserActivityFilter.determineAction("PATCH", 200)).isEqualTo("UPDATE");
        assertThat(UserActivityFilter.determineAction("DELETE", 204)).isEqualTo("DELETE");
    }

    @Test
    void determineActionMapsStatusCodes() {
        assertThat(UserActivityFilter.determineAction("GET", 401)).isEqualTo("ACCESS_DENIED");
        assertThat(UserActivityFilter.determineAction("POST", 403)).isEqualTo("ACCESS_DENIED");
        assertThat(UserActivityFilter.determineAction("GET", 429)).isEqualTo("RATE_LIMITED");
        assertThat(UserActivityFilter.determineAction("GET", 404)).isEqualTo("NOT_FOUND");
        assertThat(UserActivityFilter.determineAction("POST", 500)).isEqualTo("ERROR");
    }

    @Test
    void guessResourceTypeHandlesApiPaths() {
        assertThat(UserActivityFilter.guessResourceType("/api/v1/admin/tenants")).isEqualTo("admin/tenants");
        assertThat(UserActivityFilter.guessResourceType("/api/v1/admin/activity")).isEqualTo("admin/activity");
        assertThat(UserActivityFilter.guessResourceType("/api/v1/auth/login")).isEqualTo("auth/login");
    }

    @Test
    void guessResourceTypeHandlesSimplePaths() {
        assertThat(UserActivityFilter.guessResourceType("/health")).isEqualTo("health");
    }

    @Test
    void guessResourceTypeHandlesNull() {
        assertThat(UserActivityFilter.guessResourceType(null)).isNull();
    }
}
