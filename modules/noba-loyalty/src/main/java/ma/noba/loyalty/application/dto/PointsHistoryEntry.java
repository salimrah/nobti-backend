package ma.noba.loyalty.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class PointsHistoryEntry {
    private int points;
    private String reason;
    private Instant createdAt;
}
