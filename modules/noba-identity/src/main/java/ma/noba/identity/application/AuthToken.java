package ma.noba.identity.application;

public record AuthToken(String accessToken, String refreshToken, long expiresIn, boolean isNewUser,
                        boolean mfaRequired, String mfaToken) {

    public AuthToken(String accessToken, String refreshToken, long expiresIn, boolean isNewUser) {
        this(accessToken, refreshToken, expiresIn, isNewUser, false, null);
    }

    /** The password was correct but a TOTP code is still required before tokens are issued. */
    public static AuthToken mfaChallenge(String mfaToken) {
        return new AuthToken(null, null, 0, false, true, mfaToken);
    }
}
