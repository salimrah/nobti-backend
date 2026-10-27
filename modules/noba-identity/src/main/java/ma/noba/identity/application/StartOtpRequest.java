package ma.noba.identity.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.noba.identity.domain.model.OtpChannel;

public record StartOtpRequest(
        @NotBlank @Size(max = 255) String target,
        @NotNull OtpChannel channel,
        @NotBlank @Size(max = 20) String purpose
) {}

