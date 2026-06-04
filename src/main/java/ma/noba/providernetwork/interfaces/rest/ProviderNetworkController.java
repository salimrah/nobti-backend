package ma.noba.providernetwork.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.noba.providernetwork.application.dto.AddNetworkPartnerRequest;
import ma.noba.providernetwork.application.dto.NetworkPartnerResponse;
import ma.noba.providernetwork.application.service.ProviderNetworkApplicationService;
import ma.noba.payment.domain.port.ProviderLookupPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProviderNetworkController {

    private final ProviderNetworkApplicationService networkService;
    private final ProviderLookupPort providerLookupPort;

    @PostMapping("/api/v1/providers/self/network")
    public ResponseEntity<NetworkPartnerResponse> addPartner(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody AddNetworkPartnerRequest request
    ) {
        UUID providerId = getProviderId(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(networkService.addPartner(providerId, request));
    }

    @GetMapping("/api/v1/providers/{id}/network")
    public ResponseEntity<List<NetworkPartnerResponse>> getNetworkPartners(@PathVariable UUID id) {
        return ResponseEntity.ok(networkService.getNetworkPartners(id));
    }

    @GetMapping("/api/v1/providers/self/network")
    public ResponseEntity<List<NetworkPartnerResponse>> getMyNetwork(@AuthenticationPrincipal UUID userId) {
        UUID providerId = getProviderId(userId);
        return ResponseEntity.ok(networkService.getNetworkPartners(providerId));
    }

    @DeleteMapping("/api/v1/providers/self/network/{partnerId}")
    public ResponseEntity<Void> removePartner(@PathVariable UUID partnerId) {
        networkService.removePartner(partnerId);
        return ResponseEntity.ok().build();
    }

    private UUID getProviderId(UUID userId) {
        return providerLookupPort.findProviderIdByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No provider profile found"));
    }
}
