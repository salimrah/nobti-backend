package ma.noba.referral.domain.port;

import ma.noba.referral.domain.model.Referral;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferralRepository {
    Referral save(Referral referral);
    Optional<Referral> findById(UUID id);
    Optional<Referral> findByCode(String code);
    List<Referral> findByReferrerId(UUID referrerId);
    List<Referral> findByRefereeId(UUID refereeId);
    boolean existsByReferrerIdAndStatus(UUID referrerId, Referral.Status status);
}
