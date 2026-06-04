package ma.noba.packagegift.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.packagegift.application.dto.CreatePackageRequest;
import ma.noba.packagegift.application.dto.PackageResponse;
import ma.noba.packagegift.application.dto.UpdatePackageRequest;
import ma.noba.packagegift.domain.model.PackageServiceItem;
import ma.noba.packagegift.domain.model.ServicePackage;
import ma.noba.packagegift.domain.port.ServicePackageRepository;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PackageApplicationService {

    private final ServicePackageRepository packageRepository;

    @Transactional
    public PackageResponse createPackage(UUID providerId, CreatePackageRequest request) {
        ServicePackage pkg = new ServicePackage(providerId, request.getName(),
            request.getPrice(), request.getOriginalPrice(), request.getValidDays());
        pkg.setDescription(request.getDescription());

        if (request.getServiceItems() != null) {
            for (var item : request.getServiceItems()) {
                pkg.addServiceItem(item.getServiceId(), item.getQuantity());
            }
        }

        pkg = packageRepository.save(pkg);
        return toResponse(pkg);
    }

    public List<PackageResponse> getProviderPackages(UUID providerId) {
        return packageRepository.findByProviderId(providerId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<PackageResponse> getActiveProviderPackages(UUID providerId) {
        return packageRepository.findByProviderIdAndActive(providerId, true).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public PackageResponse updatePackage(UUID packageId, UpdatePackageRequest request) {
        ServicePackage pkg = packageRepository.findById(packageId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "Package not found: " + packageId));
        pkg.update(request.getName(), request.getDescription(),
            request.getPrice(), request.getOriginalPrice(),
            request.getValidDays() != null ? request.getValidDays() : 0);
        pkg = packageRepository.save(pkg);
        return toResponse(pkg);
    }

    @Transactional
    public void deactivatePackage(UUID packageId) {
        ServicePackage pkg = packageRepository.findById(packageId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "Package not found: " + packageId));
        pkg.deactivate();
        packageRepository.save(pkg);
    }

    private PackageResponse toResponse(ServicePackage pkg) {
        List<PackageResponse.ServiceItemResponse> items = pkg.getServiceItems().stream()
            .map(item -> new PackageResponse.ServiceItemResponse(
                item.getServiceId().toString(), item.getQuantity()))
            .toList();

        return new PackageResponse(
            pkg.getId().toString(), pkg.getProviderId().toString(),
            pkg.getName(), pkg.getDescription(),
            pkg.getPrice(), pkg.getOriginalPrice(),
            pkg.getValidDays(), pkg.isActive(), items);
    }
}
