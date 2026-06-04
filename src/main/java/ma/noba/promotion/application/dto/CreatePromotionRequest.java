package ma.noba.promotion.application.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
public class CreatePromotionRequest {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    @DecimalMin("0.01")
    @DecimalMax("100.00")
    private BigDecimal discountPercent;

    @NotNull
    private Instant validFrom;

    @NotNull
    private Instant validTo;

    @Positive
    private Integer maxRedemptions;

    private List<String> serviceIds;
}
