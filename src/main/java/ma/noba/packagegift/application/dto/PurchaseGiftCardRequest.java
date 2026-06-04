package ma.noba.packagegift.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PurchaseGiftCardRequest {
    @NotNull
    private UUID providerId;

    @NotNull
    @Positive
    private BigDecimal amount;

    private String recipientName;

    private String message;
}
