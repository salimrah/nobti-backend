package ma.noba.app.statement;

import lombok.RequiredArgsConstructor;
import ma.noba.referral.domain.port.ReferralRepository;
import ma.noba.statement.domain.port.StatementReferralSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StatementReferralAdapter implements StatementReferralSource {

    private final ReferralRepository referrals;

    @Override
    public List<ReferralLine> findReferrals(UUID referrerId) {
        return referrals.findByReferrerId(referrerId).stream()
                .map(referral -> new ReferralLine(
                        referral.getCreatedAt(),
                        referral.getStatus() != null ? referral.getStatus().name() : null,
                        referral.getId()))
                .toList();
    }
}
