package ma.noba.loyalty.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LoyaltyResponse {
    private String providerId;
    private String providerName;
    private int totalPoints;
    private String tier;
    private List<LoyaltyRewardResponse> availableRewards;
    private List<PointsHistoryEntry> recentHistory;
}
