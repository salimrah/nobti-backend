package ma.noba.payment.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CreateCheckoutRequest {
    @NotNull
    private String providerId;

    private String serviceId;

    @NotBlank
    private String currency;

    @NotNull
    @DecimalMin("1.00")
    private BigDecimal amount;

    private String returnUrl;
}
