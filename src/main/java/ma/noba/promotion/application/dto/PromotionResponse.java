package ma.noba.promotion.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
public class PromotionResponse {
    private String id;
    private String providerId;
    private String title;
    private String description;
    private BigDecimal discountPercent;
    private Instant validFrom;
    private Instant validTo;
    private Integer maxRedemptions;
    private int currentRedemptions;
    private boolean active;
    private boolean valid;
    private List<String> serviceIds;
    private Instant createdAt;
}
