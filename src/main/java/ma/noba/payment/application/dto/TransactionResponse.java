package ma.noba.payment.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
public class TransactionResponse {
    private String id;
    private String userId;
    private String providerId;
    private String serviceId;
    private String currency;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal netAmount;
    private String status;
    private String stripePaymentIntentId;
    private String failureMessage;
    private Instant createdAt;
    private Instant completedAt;
}
