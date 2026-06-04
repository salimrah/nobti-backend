package ma.noba.employee.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateLocationRequest {
    @NotBlank
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private String phone;
}
