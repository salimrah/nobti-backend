package ma.noba.identity.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum SocialProvider {
    GOOGLE, FACEBOOK, APPLE;

    @JsonCreator
    public static SocialProvider fromString(String value) {
        if (value == null) return null;
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
