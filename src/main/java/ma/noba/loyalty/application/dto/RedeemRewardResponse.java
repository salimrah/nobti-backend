package ma.noba.loyalty.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RedeemRewardResponse {
    private String redemptionId;
    private String rewardName;
    private int pointsSpent;
    private String status;
    private String message;
}
