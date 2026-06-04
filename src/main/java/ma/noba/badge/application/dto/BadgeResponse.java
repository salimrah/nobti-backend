package ma.noba.badge.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class BadgeResponse {
    private String id;
    private String providerId;
    private String badgeType;
    private Instant awardedAt;
    private Instant expiresAt;
    private boolean autoAwarded;
}
