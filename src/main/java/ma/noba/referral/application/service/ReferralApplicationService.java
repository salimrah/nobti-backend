package ma.noba.referral.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.referral.application.dto.*;
import ma.noba.referral.domain.model.Referral;
import ma.noba.referral.domain.port.ReferralRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReferralApplicationService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ReferralRepository referralRepository;

    @Transactional
    public CreateReferralResponse createReferral(UUID userId) {
        String code = generateCode();
        Referral referral = new Referral(userId, code);
        referral = referralRepository.save(referral);

        return new CreateReferralResponse(
            referral.getId().toString(),
            referral.getCode(),
            "https://noba.app/refer/" + referral.getCode()
        );
    }

    @Transactional
    public ReferralResponse claimReferral(UUID userId, String code) {
        Referral referral = referralRepository.findByCode(code.toUpperCase())
            .orElseThrow(() -> new IllegalArgumentException("Invalid referral code"));

        if (referral.getStatus() != Referral.Status.PENDING) {
            throw new IllegalStateException("Referral code has already been used or expired");
        }

        if (referral.getReferrerId().equals(userId)) {
            throw new IllegalArgumentException("Cannot use your own referral code");
        }

        referral.complete(userId, "PRIORITY_PASS");
        referral = referralRepository.save(referral);

        return toResponse(referral);
    }

    public List<ReferralResponse> getMyReferrals(UUID userId) {
        return referralRepository.findByReferrerId(userId).stream()
            .map(ReferralApplicationService::toResponse)
            .toList();
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public static ReferralResponse toResponse(Referral r) {
        return new ReferralResponse(
            r.getId().toString(),
            r.getReferrerId().toString(),
            r.getRefereeId() != null ? r.getRefereeId().toString() : null,
            r.getCode(),
            r.getStatus().name(),
            r.isRewardClaimed(),
            r.getRewardType(),
            r.getCreatedAt()
        );
    }
}
