package ma.noba.identity.domain.model;

public record AuthToken(String accessToken, String refreshToken, long expiresIn, boolean isNewUser) {
}
