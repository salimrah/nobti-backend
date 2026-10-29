package ma.noba.admin.application.dto;

import java.math.BigDecimal;

/** A null amount refunds everything still refundable. */
public record RefundRequest(BigDecimal amount, String reason) {
}
