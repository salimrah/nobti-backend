package ma.noba.referral.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClaimReferralRequest {
    @NotBlank
    private String code;
}
