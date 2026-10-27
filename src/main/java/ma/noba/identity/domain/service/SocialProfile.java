package ma.noba.identity.domain.service;

/**
 * Verified profile returned by a social/OIDC provider.
 *
 * @param emailVerified whether the provider asserts the email is verified. Account linking by email
 *                      must only happen when this is true, otherwise an attacker who can present an
 *                      unverified email equal to a victim's could take over the account.
 */
public record SocialProfile(String subject, String email, String name, boolean emailVerified) {}
