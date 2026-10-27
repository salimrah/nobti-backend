package ma.noba.referral.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateReferralResponse {
    private String id;
    private String code;
    private String shareLink;
}
