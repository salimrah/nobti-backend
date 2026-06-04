package ma.noba.providernetwork.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AddNetworkPartnerRequest {
    @NotNull
    private UUID partnerId;

    @NotBlank
    private String partnerName;

    private String relationshipType;
}
