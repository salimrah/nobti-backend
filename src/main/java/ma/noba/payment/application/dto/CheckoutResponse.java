package ma.noba.payment.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckoutResponse {
    private String paymentIntentId;
    private String clientSecret;
    private String transactionId;
    private String status;
}
