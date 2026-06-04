package ma.noba.providernetwork.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.providernetwork.application.dto.AddNetworkPartnerRequest;
import ma.noba.providernetwork.application.dto.NetworkPartnerResponse;
import ma.noba.providernetwork.domain.model.ProviderNetwork;
import ma.noba.providernetwork.domain.port.ProviderNetworkRepository;
import ma.noba.shared.domain.exception.BusinessException;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProviderNetworkApplicationService {

    private final ProviderNetworkRepository networkRepository;

    @Transactional
    public NetworkPartnerResponse addPartner(UUID providerId, AddNetworkPartnerRequest request) {
        if (providerId.equals(request.getPartnerId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Cannot add yourself as a partner");
        }

        if (networkRepository.findByProviderIdAndPartnerId(providerId, request.getPartnerId()).isPresent()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Partner already exists in network");
        }

        ProviderNetwork network = new ProviderNetwork(providerId, request.getPartnerId(),
            request.getPartnerName(), request.getRelationshipType());
        network = networkRepository.save(network);

        return toResponse(network);
    }

    public List<NetworkPartnerResponse> getNetworkPartners(UUID providerId) {
        return networkRepository.findByProviderId(providerId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public void removePartner(UUID partnerId) {
        ProviderNetwork network = networkRepository.findById(partnerId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "Network partner not found: " + partnerId));
        networkRepository.delete(network);
    }

    private NetworkPartnerResponse toResponse(ProviderNetwork n) {
        return new NetworkPartnerResponse(
            n.getId().toString(), n.getProviderId().toString(),
            n.getPartnerId().toString(), n.getPartnerName(),
            n.getRelationshipType(), n.getCreatedAt().toString());
    }
}
