package ma.noba.payment.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
public class PayoutResponse {
    private String id;
    private String providerId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private Instant createdAt;
    private Instant paidAt;
}
