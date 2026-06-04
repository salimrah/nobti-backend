package ma.noba.employee.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignLocationRequest {
    @NotBlank
    private String locationId;
}
