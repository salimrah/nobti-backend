package ma.noba.employee.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LocationResponse {
    private String id;
    private String providerId;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private String phone;
    private boolean active;
}
