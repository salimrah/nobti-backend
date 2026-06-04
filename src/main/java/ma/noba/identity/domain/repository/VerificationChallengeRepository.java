package ma.noba.identity.domain.repository;

import ma.noba.identity.domain.model.VerificationChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationChallengeRepository extends JpaRepository<VerificationChallenge, UUID> {
    Optional<VerificationChallenge> findByChallengeTypeAndCodeHashAndConsumedAtIsNull(String challengeType, String codeHash);
}
