package ma.noba.media.infra;

import ma.noba.media.domain.model.ReviewMedia;
import ma.noba.media.domain.port.ReviewMediaRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaReviewMediaRepository extends JpaRepository<ReviewMedia, UUID>, ReviewMediaRepository {
    List<ReviewMedia> findByReviewId(UUID reviewId);
}
