package ma.noba.employee.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateEmployeeRequest {
    @NotBlank
    private String name;
    private String role;
}
