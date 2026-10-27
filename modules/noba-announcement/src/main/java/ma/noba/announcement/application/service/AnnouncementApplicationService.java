package ma.noba.announcement.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.announcement.application.dto.AnnouncementResponse;
import ma.noba.announcement.application.dto.CreateAnnouncementRequest;
import ma.noba.announcement.domain.model.Announcement;
import ma.noba.announcement.domain.port.AnnouncementRepository;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnnouncementApplicationService {

    private final AnnouncementRepository announcementRepository;

    @Transactional
    public AnnouncementResponse createAnnouncement(UUID providerId, CreateAnnouncementRequest request) {
        Instant expiresAt = request.getExpiresAt() != null ? Instant.parse(request.getExpiresAt()) : null;
        Announcement ann = new Announcement(providerId, request.getTitle(),
            request.getMessage(), request.isImportant(), expiresAt);
        ann = announcementRepository.save(ann);
        return toResponse(ann);
    }

    public List<AnnouncementResponse> getProviderAnnouncements(UUID providerId) {
        return announcementRepository
            .findByProviderIdAndExpiresAtAfterOrExpiresAtIsNullOrderByCreatedAtDesc(providerId, Instant.now())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<AnnouncementResponse> getAllProviderAnnouncements(UUID providerId) {
        return announcementRepository.findByProviderIdOrderByCreatedAtDesc(providerId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public void deleteAnnouncement(UUID id) {
        Announcement ann = announcementRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "Announcement not found: " + id));
        announcementRepository.delete(ann);
    }

    private AnnouncementResponse toResponse(Announcement a) {
        return new AnnouncementResponse(
            a.getId().toString(), a.getProviderId().toString(),
            a.getTitle(), a.getMessage(), a.isImportant(),
            a.getCreatedAt().toString(),
            a.getExpiresAt() != null ? a.getExpiresAt().toString() : null);
    }
}
