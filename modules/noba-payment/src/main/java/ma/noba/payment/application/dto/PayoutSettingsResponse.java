package ma.noba.payment.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PayoutSettingsResponse {
    private String providerId;
    private boolean payoutsEnabled;
    private boolean stripeAccountLinked;
    private String stripeAccountLink;
}
