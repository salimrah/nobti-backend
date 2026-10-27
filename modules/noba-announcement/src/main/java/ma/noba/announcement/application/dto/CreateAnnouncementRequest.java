package ma.noba.announcement.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAnnouncementRequest {
    @NotBlank
    private String title;

    private String message;

    private boolean important;

    private String expiresAt;
}
