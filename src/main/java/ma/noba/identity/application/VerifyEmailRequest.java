package ma.noba.identity.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(@NotBlank @Email @Size(max = 255) String email, @NotBlank String code) {}
