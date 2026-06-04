package ma.noba.providernetwork.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NetworkPartnerResponse {
    private String id;
    private String providerId;
    private String partnerId;
    private String partnerName;
    private String relationshipType;
    private String createdAt;
}
