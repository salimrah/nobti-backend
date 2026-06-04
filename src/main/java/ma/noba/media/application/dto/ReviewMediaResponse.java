package ma.noba.media.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ReviewMediaResponse {
    private String id;
    private String reviewId;
    private String url;
    private String mediaType;
    private boolean moderated;
    private Instant uploadedAt;
}
