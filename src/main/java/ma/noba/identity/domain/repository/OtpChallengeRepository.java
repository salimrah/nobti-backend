package ma.noba.identity.domain.repository;

import ma.noba.identity.domain.model.OtpChallenge;

import java.time.Duration;
import java.util.Optional;

public interface OtpChallengeRepository {
    void save(OtpChallenge challenge, Duration ttl);
    Optional<OtpChallenge> find(String id);
    boolean delete(String id);
}
