package ma.noba.media.domain.port;

import ma.noba.media.domain.model.ReviewMedia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewMediaRepository {
    ReviewMedia save(ReviewMedia media);
    Optional<ReviewMedia> findById(UUID id);
    List<ReviewMedia> findByReviewId(UUID reviewId);
    void delete(ReviewMedia media);
}
