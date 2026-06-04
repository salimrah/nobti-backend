package ma.noba.employee.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.employee.application.dto.CreateEmployeeRequest;
import ma.noba.employee.application.dto.EmployeeResponse;
import ma.noba.employee.application.dto.UpdateEmployeeRequest;
import ma.noba.employee.domain.model.ProviderEmployee;
import ma.noba.employee.domain.port.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeApplicationService {

    private final EmployeeRepository employeeRepository;

    @Transactional
    public EmployeeResponse addEmployee(UUID providerId, CreateEmployeeRequest request) {
        ProviderEmployee employee = new ProviderEmployee(providerId, request.getName(), request.getRole());
        employee = employeeRepository.save(employee);
        return toResponse(employee);
    }

    public List<EmployeeResponse> getEmployees(UUID providerId) {
        return employeeRepository.findByProviderId(providerId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<EmployeeResponse> getActiveEmployees(UUID providerId) {
        return employeeRepository.findByProviderIdAndIsActive(providerId, true).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public EmployeeResponse updateEmployee(UUID employeeId, UpdateEmployeeRequest request) {
        ProviderEmployee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        employee.update(request.getName(), request.getRole());
        employee = employeeRepository.save(employee);
        return toResponse(employee);
    }

    @Transactional
    public void removeEmployee(UUID employeeId) {
        ProviderEmployee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        employee.deactivate();
        employeeRepository.save(employee);
    }

    @Transactional
    public EmployeeResponse assignLocation(UUID employeeId, UUID locationId) {
        ProviderEmployee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        employee.assignLocation(locationId);
        employee = employeeRepository.save(employee);
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse removeLocation(UUID employeeId) {
        ProviderEmployee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        employee.removeLocation();
        employee = employeeRepository.save(employee);
        return toResponse(employee);
    }

    private EmployeeResponse toResponse(ProviderEmployee e) {
        return new EmployeeResponse(
            e.getId().toString(), e.getProviderId().toString(),
            e.getUserId() != null ? e.getUserId().toString() : null,
            e.getLocationId() != null ? e.getLocationId().toString() : null,
            e.getName(), e.getRole(), e.isActive()
        );
    }
}
