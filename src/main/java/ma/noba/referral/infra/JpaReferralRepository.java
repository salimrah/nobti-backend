package ma.noba.referral.infra;

import ma.noba.referral.domain.model.Referral;
import ma.noba.referral.domain.port.ReferralRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaReferralRepository extends JpaRepository<Referral, UUID>, ReferralRepository {
    Optional<Referral> findByCode(String code);
    List<Referral> findByReferrerId(UUID referrerId);
    List<Referral> findByRefereeId(UUID refereeId);
    boolean existsByReferrerIdAndStatus(UUID referrerId, Referral.Status status);
}
