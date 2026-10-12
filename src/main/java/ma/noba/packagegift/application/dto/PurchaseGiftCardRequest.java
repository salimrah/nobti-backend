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

    /**
     * The SUCCEEDED payment transaction that funds this gift card. Issuance is gated on a
     * verified payment so gift cards can no longer be minted without paying.
     */
    @NotNull
    private UUID paymentTransactionId;

    private String recipientName;

    private String message;
}
