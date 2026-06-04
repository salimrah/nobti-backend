package ma.noba.employee.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmployeeResponse {
    private String id;
    private String providerId;
    private String userId;
    private String locationId;
    private String name;
    private String role;
    private boolean active;
}
