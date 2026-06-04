package ma.noba.identity.application;

public record AuthToken(String accessToken, String refreshToken, long expiresIn, boolean isNewUser) {
}
