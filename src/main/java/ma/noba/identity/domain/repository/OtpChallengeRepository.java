package ma.noba.identity.domain.repository;

import ma.noba.identity.domain.model.OtpChallenge;

import java.time.Duration;
import java.util.Optional;

public interface OtpChallengeRepository {
    void save(OtpChallenge challenge, Duration ttl);
    Optional<OtpChallenge> find(String id);
    boolean delete(String id);

    /**
     * Atomically increments and returns the attempt counter for a challenge. Atomicity is required:
     * a read-modify-write on the challenge object lets concurrent guesses each see the old count and
     * bypass {@code maxAttempts}.
     */
    int incrementAttempts(String id, Duration ttl);
}
