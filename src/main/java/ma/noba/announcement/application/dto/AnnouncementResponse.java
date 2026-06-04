package ma.noba.announcement.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnnouncementResponse {
    private String id;
    private String providerId;
    private String title;
    private String message;
    private boolean important;
    private String createdAt;
    private String expiresAt;
}
