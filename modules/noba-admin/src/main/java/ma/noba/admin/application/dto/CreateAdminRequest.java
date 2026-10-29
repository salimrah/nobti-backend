package ma.noba.admin.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAdminRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 12, max = 128) String password,
        String firstName,
        String lastName) {
}
