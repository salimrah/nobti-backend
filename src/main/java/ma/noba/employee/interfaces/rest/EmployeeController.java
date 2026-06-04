package ma.noba.employee.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.noba.employee.application.dto.AssignLocationRequest;
import ma.noba.employee.application.dto.CreateEmployeeRequest;
import ma.noba.employee.application.dto.EmployeeResponse;
import ma.noba.employee.application.dto.UpdateEmployeeRequest;
import ma.noba.employee.application.service.EmployeeApplicationService;
import ma.noba.payment.domain.port.ProviderLookupPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/providers/self/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeApplicationService employeeService;
    private final ProviderLookupPort providerLookupPort;

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getEmployees(@AuthenticationPrincipal UUID userId) {
        UUID providerId = getProviderId(userId);
        return ResponseEntity.ok(employeeService.getEmployees(providerId));
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> addEmployee(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody CreateEmployeeRequest request
    ) {
        UUID providerId = getProviderId(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(employeeService.addEmployee(providerId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeEmployee(@PathVariable UUID id) {
        employeeService.removeEmployee(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/location")
    public ResponseEntity<EmployeeResponse> assignLocation(
        @PathVariable UUID id,
        @RequestBody AssignLocationRequest request
    ) {
        return ResponseEntity.ok(employeeService.assignLocation(id, UUID.fromString(request.getLocationId())));
    }

    @DeleteMapping("/{id}/location")
    public ResponseEntity<EmployeeResponse> removeLocation(@PathVariable UUID id) {
        return ResponseEntity.ok(employeeService.removeLocation(id));
    }

    private UUID getProviderId(UUID userId) {
        return providerLookupPort.findProviderIdByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No provider profile found"));
    }
}
