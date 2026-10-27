package ma.noba.loyalty.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class LoyaltyRewardResponse {
    private String id;
    private String providerId;
    private String name;
    private String description;
    private int pointsRequired;
    private boolean active;
    private Instant createdAt;
}
