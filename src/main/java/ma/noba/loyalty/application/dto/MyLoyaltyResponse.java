package ma.noba.loyalty.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MyLoyaltyResponse {
    private List<LoyaltyResponse> providers;
    private int totalPointsAcrossAll;
    private List<PointsHistoryEntry> allHistory;
}
