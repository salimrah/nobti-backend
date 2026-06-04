package ma.noba.media.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.media.application.dto.AddMediaRequest;
import ma.noba.media.application.dto.AddProviderMediaRequest;
import ma.noba.media.application.dto.ProviderMediaResponse;
import ma.noba.media.application.dto.ReviewMediaResponse;
import ma.noba.media.domain.model.ProviderMedia;
import ma.noba.media.domain.model.ReviewMedia;
import ma.noba.media.domain.port.ProviderMediaRepository;
import ma.noba.media.domain.port.ReviewMediaRepository;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaApplicationService {

    private final ReviewMediaRepository mediaRepository;
    private final ProviderMediaRepository providerMediaRepository;

    public List<ReviewMediaResponse> getReviewMedia(UUID reviewId) {
        return mediaRepository.findByReviewId(reviewId).stream()
            .map(MediaApplicationService::toResponse)
            .toList();
    }

    @Transactional
    public ReviewMediaResponse addMedia(UUID reviewId, AddMediaRequest request) {
        ReviewMedia media = new ReviewMedia(reviewId, request.getUrl(), request.getMediaType());
        media = mediaRepository.save(media);
        return toResponse(media);
    }

    @Transactional
    public void removeMedia(UUID mediaId) {
        ReviewMedia media = mediaRepository.findById(mediaId)
            .orElseThrow(() -> new IllegalArgumentException("Media not found: " + mediaId));
        mediaRepository.delete(media);
    }

    @Transactional
    public void markModerated(UUID mediaId) {
        ReviewMedia media = mediaRepository.findById(mediaId)
            .orElseThrow(() -> new IllegalArgumentException("Media not found: " + mediaId));
        media.markModerated();
        mediaRepository.save(media);
    }

    public List<ProviderMediaResponse> getProviderMedia(UUID providerId) {
        return providerMediaRepository.findByProviderIdOrderByMediaOrderAsc(providerId).stream()
            .map(MediaApplicationService::toProviderResponse)
            .toList();
    }

    @Transactional
    public ProviderMediaResponse addProviderMedia(UUID providerId, AddProviderMediaRequest request) {
        int nextOrder = request.getMediaOrder() != null
            ? request.getMediaOrder()
            : (int) providerMediaRepository.findByProviderIdOrderByMediaOrderAsc(providerId).size();
        ProviderMedia media = new ProviderMedia(providerId, request.getUrl(),
            request.getCaption(), request.getCategory(), nextOrder);
        media = providerMediaRepository.save(media);
        return toProviderResponse(media);
    }

    @Transactional
    public void removeProviderMedia(UUID mediaId) {
        ProviderMedia media = providerMediaRepository.findById(mediaId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "Provider media not found: " + mediaId));
        providerMediaRepository.delete(media);
    }

    @Transactional
    public ProviderMediaResponse updateProviderMediaOrder(UUID mediaId, int newOrder) {
        ProviderMedia media = providerMediaRepository.findById(mediaId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "Provider media not found: " + mediaId));
        media.updateOrder(newOrder);
        media = providerMediaRepository.save(media);
        return toProviderResponse(media);
    }

    public static ReviewMediaResponse toResponse(ReviewMedia m) {
        return new ReviewMediaResponse(
            m.getId().toString(), m.getReviewId().toString(),
            m.getUrl(), m.getMediaType(), m.isModerated(), m.getUploadedAt()
        );
    }

    public static ProviderMediaResponse toProviderResponse(ProviderMedia m) {
        return new ProviderMediaResponse(
            m.getId().toString(), m.getProviderId().toString(),
            m.getUrl(), m.getCaption(), m.getCategory(),
            m.getMediaOrder(), m.getCreatedAt().toString()
        );
    }
}
