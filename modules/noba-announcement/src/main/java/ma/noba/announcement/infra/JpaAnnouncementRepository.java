package ma.noba.announcement.infra;

import ma.noba.announcement.domain.model.Announcement;
import ma.noba.announcement.domain.port.AnnouncementRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JpaAnnouncementRepository extends JpaRepository<Announcement, UUID>, AnnouncementRepository {

    List<Announcement> findByProviderIdOrderByCreatedAtDesc(UUID providerId);

    @Query("SELECT a FROM Announcement a WHERE a.providerId = :providerId AND (a.expiresAt IS NULL OR a.expiresAt > :now) ORDER BY a.createdAt DESC")
    List<Announcement> findByProviderIdAndExpiresAtAfterOrExpiresAtIsNullOrderByCreatedAtDesc(UUID providerId, Instant now);
}
