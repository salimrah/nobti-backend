package ma.noba.announcement.domain.port;

import ma.noba.announcement.domain.model.Announcement;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnnouncementRepository {
    Announcement save(Announcement announcement);
    Optional<Announcement> findById(UUID id);
    List<Announcement> findByProviderIdOrderByCreatedAtDesc(UUID providerId);
    List<Announcement> findByProviderIdAndExpiresAtAfterOrExpiresAtIsNullOrderByCreatedAtDesc(UUID providerId, Instant now);
    void delete(Announcement announcement);
}
