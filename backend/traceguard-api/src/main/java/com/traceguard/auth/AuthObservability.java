package com.traceguard.auth;

public final class AuthObservability {

    public static final String EVENT_AUTH_LOGIN_SUCCESS = "AUTH_LOGIN_SUCCESS";
    public static final String EVENT_AUTH_LOGIN_FAILURE = "AUTH_LOGIN_FAILURE";
    public static final String EVENT_AUTH_REFRESH_SUCCESS = "AUTH_REFRESH_SUCCESS";
    public static final String EVENT_AUTH_REFRESH_FAILURE = "AUTH_REFRESH_FAILURE";
    public static final String EVENT_AUTH_REFRESH_REUSE_DETECTED = "AUTH_REFRESH_REUSE_DETECTED";
    public static final String EVENT_AUTH_LOGOUT_SUCCESS = "AUTH_LOGOUT_SUCCESS";
    public static final String EVENT_AUTH_LOGOUT_FAILURE = "AUTH_LOGOUT_FAILURE";
    public static final String EVENT_AUTH_UNAUTHORIZED = "AUTH_UNAUTHORIZED";
    public static final String EVENT_AUTH_FORBIDDEN = "AUTH_FORBIDDEN";
    public static final String EVENT_AUTH_JWT_VALIDATION_FAILURE = "AUTH_JWT_VALIDATION_FAILURE";

    public static final String FLOW_LOGIN = "login";
    public static final String FLOW_REFRESH = "refresh";
    public static final String FLOW_LOGOUT = "logout";

    public static final String OUTCOME_SUCCESS = "success";
    public static final String OUTCOME_FAILURE = "failure";

    public static final String REASON_NONE = "none";
    public static final String REASON_INVALID_CREDENTIALS = "invalid_credentials";
    public static final String REASON_INVALID_REFRESH_TOKEN = "invalid_refresh_token";
    public static final String REASON_EXPIRED_TOKEN = "expired_token";
    public static final String REASON_REUSE_DETECTED = "reuse_detected";
    public static final String REASON_USER_INACTIVE = "user_inactive";
    public static final String REASON_TOKEN_NOT_FOUND = "token_not_found";
    public static final String REASON_UNAUTHORIZED = "unauthorized";
    public static final String REASON_FORBIDDEN = "forbidden";
    public static final String REASON_JWT_INVALID_SIGNATURE = "jwt_invalid_signature";
    public static final String REASON_JWT_EXPIRED = "jwt_expired";
    public static final String REASON_JWT_MALFORMED = "jwt_malformed";
    public static final String REASON_JWT_INVALID_ISSUER = "jwt_invalid_issuer";
    public static final String REASON_JWT_INVALID_AUDIENCE = "jwt_invalid_audience";
    public static final String REASON_JWT_MISSING_CLAIM = "jwt_missing_claim";
    public static final String REASON_JWT_INVALID_CLAIM = "jwt_invalid_claim";

    private AuthObservability() {
    }
}
