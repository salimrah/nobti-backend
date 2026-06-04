package ma.noba.packagegift.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class GiftCardResponse {
    private String id;
    private String providerId;
    private String buyerId;
    private String redeemerId;
    private String recipientName;
    private BigDecimal amount;
    private String code;
    private String message;
    private boolean redeemed;
    private String redeemedAt;
    private String createdAt;
}
