package ma.noba.provider.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Editable per-provider configuration. */
public record ProviderSettingsRequest(
        @Min(1) @Max(50) int maxPartySize,
        boolean walkInsEnabled,
        boolean appointmentsEnabled,
        @Min(0) @Max(120) int noShowGraceMinutes,
        @Min(0) @Max(1440) int cancellationWindowMinutes,
        boolean autoFillEnabled,
        @Min(0) @Max(1000) int loyaltyPointsPerVisit,
        @Size(max = 8) String notificationLocale
) {}
