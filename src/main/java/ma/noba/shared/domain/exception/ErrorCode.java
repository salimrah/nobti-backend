package ma.noba.shared.domain.exception;

public enum ErrorCode {
    AUTH_INVALID_CREDENTIALS("AUTH_001"),
    AUTH_TOKEN_EXPIRED("AUTH_002"),
    AUTH_TOKEN_INVALID("AUTH_003"),
    AUTH_RATE_LIMIT_EXCEEDED("AUTH_004"),
    AUTH_ACCOUNT_LOCKED("AUTH_005"),
    AUTH_ACCOUNT_SUSPENDED("AUTH_006"),
    AUTH_TOKEN_REUSE("AUTH_007"),
    AUTH_ALREADY_LINKED("AUTH_008"),
    AUTH_LAST_METHOD("AUTH_009"),
    AUTH_SUSPICIOUS_ACTIVITY("AUTH_010"),
    AUTH_NO_SUCH_ACCOUNT("AUTH_011"),

    OTP_INVALID_CODE("OTP_001"),
    OTP_EXPIRED("OTP_002"),
    OTP_RATE_LIMIT_EXCEEDED("OTP_003"),
    OTP_ALREADY_VERIFIED("OTP_004"),

    EMAIL_TAKEN("USR_004"),
    PASSWORD_WEAK("USR_005"),
    EMAIL_NOT_VERIFIED("USR_006"),
    INVALID_RESET_TOKEN("USR_007"),

    USER_NOT_FOUND("USR_001"),
    USER_ALREADY_EXISTS("USR_002"),
    USER_SUSPENDED("USR_003"),

    QUEUE_NOT_FOUND("QUE_001"),
    QUEUE_INACTIVE("QUE_002"),
    QUEUE_ALREADY_JOINED("QUE_003"),
    QUEUE_FULL("QUE_004"),
    QUEUE_CLOSED("QUE_005"),

    PROVIDER_NOT_FOUND("PRV_001"),
    PROVIDER_ALREADY_EXISTS("PRV_002"),
    PROVIDER_SERVICE_NOT_FOUND("PRV_003"),
    PROVIDER_PENDING("PRV_004"),

    SUBSCRIPTION_NOT_FOUND("SUB_001"),
    TIER_LIMIT_EXCEEDED("SUB_002"),

    QUESTION_NOT_FOUND("QA_001"),

    RATE_LIMIT_EXCEEDED("GEN_004"),
    NOT_FOUND("GEN_001"),
    VALIDATION_ERROR("GEN_002"),
    INTERNAL_ERROR("GEN_003");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return switch (this) {
            case AUTH_INVALID_CREDENTIALS -> "Invalid credentials";
            case AUTH_TOKEN_EXPIRED -> "Token has expired";
            case AUTH_TOKEN_INVALID -> "Token is invalid";
            case AUTH_RATE_LIMIT_EXCEEDED -> "Rate limit exceeded for authentication";
            case AUTH_ACCOUNT_LOCKED -> "Account is locked";
            case AUTH_ACCOUNT_SUSPENDED -> "Account is suspended";
            case AUTH_TOKEN_REUSE -> "Refresh token reuse detected";
            case AUTH_ALREADY_LINKED -> "Account already linked to this provider";
            case AUTH_LAST_METHOD -> "Cannot unlink the last authentication method";
            case AUTH_SUSPICIOUS_ACTIVITY -> "Suspicious activity detected";
            case AUTH_NO_SUCH_ACCOUNT -> "No such account linked";
            case OTP_INVALID_CODE -> "Invalid OTP code";
            case OTP_EXPIRED -> "OTP has expired";
            case OTP_RATE_LIMIT_EXCEEDED -> "OTP rate limit exceeded";
            case OTP_ALREADY_VERIFIED -> "OTP already verified";
            case EMAIL_TAKEN -> "Email is already taken";
            case PASSWORD_WEAK -> "Password does not meet requirements";
            case EMAIL_NOT_VERIFIED -> "Email is not verified";
            case INVALID_RESET_TOKEN -> "Invalid reset token";
            case USER_NOT_FOUND -> "User not found";
            case USER_ALREADY_EXISTS -> "User already exists";
            case USER_SUSPENDED -> "User is suspended";
            case QUEUE_NOT_FOUND -> "Queue not found";
            case QUEUE_INACTIVE -> "Queue is inactive";
            case QUEUE_ALREADY_JOINED -> "Already joined this queue";
            case QUEUE_FULL -> "Queue is full";
            case QUEUE_CLOSED -> "Queue is closed";
            case PROVIDER_NOT_FOUND -> "Provider not found";
            case PROVIDER_ALREADY_EXISTS -> "Provider profile already exists for this user";
            case PROVIDER_SERVICE_NOT_FOUND -> "Provider service not found";
            case PROVIDER_PENDING -> "Provider profile is pending approval";
            case SUBSCRIPTION_NOT_FOUND -> "No subscription found for this provider";
            case TIER_LIMIT_EXCEEDED -> "Your current plan does not support this feature";
            case QUESTION_NOT_FOUND -> "Question not found";
            case RATE_LIMIT_EXCEEDED -> "Rate limit exceeded";
            case NOT_FOUND -> "Resource not found";
            case VALIDATION_ERROR -> "Validation error";
            case INTERNAL_ERROR -> "Internal server error";
        };
    }
}
