package ma.noba.badge.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AwardBadgeRequest {
    @NotBlank
    private String providerId;

    @NotNull
    private String badgeType;
}
