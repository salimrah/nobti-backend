package ma.noba.referral.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ReferralResponse {
    private String id;
    private String referrerId;
    private String refereeId;
    private String code;
    private String status;
    private boolean rewardClaimed;
    private String rewardType;
    private Instant createdAt;
}
