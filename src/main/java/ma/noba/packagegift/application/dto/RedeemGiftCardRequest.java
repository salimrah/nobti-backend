package ma.noba.packagegift.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RedeemGiftCardRequest {
    @NotBlank
    private String code;
}
