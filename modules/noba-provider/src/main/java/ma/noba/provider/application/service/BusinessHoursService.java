package ma.noba.provider.application.service;

import ma.noba.provider.application.dto.BusinessHoursRequest;
import ma.noba.provider.application.dto.DayHours;
import ma.noba.provider.domain.model.BusinessHours;
import ma.noba.provider.domain.port.service.BusinessHoursRepository;
import ma.noba.provider.domain.port.service.ProviderRepository;
import ma.noba.shared.domain.exception.NotFoundException;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BusinessHoursService {

    private static final Map<Integer, String> DAY_NAMES = Map.of(
            1, "Monday", 2, "Tuesday", 3, "Wednesday",
            4, "Thursday", 5, "Friday", 6, "Saturday", 7, "Sunday"
    );

    private final BusinessHoursRepository businessHoursRepository;
    private final ProviderRepository providerRepository;

    public BusinessHoursService(BusinessHoursRepository businessHoursRepository, ProviderRepository providerRepository) {
        this.businessHoursRepository = businessHoursRepository;
        this.providerRepository = providerRepository;
    }

    @Transactional
    public List<DayHours> setBusinessHours(UUID providerId, BusinessHoursRequest request) {
        providerRepository.findById(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PROVIDER_NOT_FOUND, "Provider not found"));

        businessHoursRepository.deleteByProviderId(providerId);

        for (var d : request.days()) {
            if (!d.closed() && d.opensAt() != null && d.closesAt() != null && !d.opensAt().isBefore(d.closesAt())) {
                throw new ValidationException(ErrorCode.VALIDATION_ERROR,
                        "opensAt must be before closesAt on " + d.dayOfWeek());
            }
        }

        List<BusinessHours> hours = request.days().stream()
                .map(d -> d.closed()
                        ? BusinessHours.closedDay(providerId, d.dayOfWeek())
                        : BusinessHours.workingDay(providerId, d.dayOfWeek(), d.opensAt(), d.closesAt()))
                .toList();

        List<BusinessHours> saved = businessHoursRepository.persistAll(hours);
        return saved.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DayHours> getBusinessHours(UUID providerId) {
        providerRepository.findById(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PROVIDER_NOT_FOUND, "Provider not found"));

        Map<Integer, BusinessHours> existing = businessHoursRepository.findByProviderId(providerId).stream()
                .collect(java.util.stream.Collectors.toMap(BusinessHours::getDayOfWeek, h -> h));

        return java.util.Arrays.stream(DayOfWeek.values())
                .map(dow -> {
                    int dayNum = dow.getValue();
                    BusinessHours hours = existing.get(dayNum);
                    if (hours != null) {
                        return toResponse(hours);
                    }
                    return new DayHours(dayNum, DAY_NAMES.get(dayNum), null, null, true);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isOpenNow(UUID providerId) {
        var now = java.time.LocalTime.now();
        var today = java.time.LocalDate.now().getDayOfWeek().getValue();

        return businessHoursRepository.findByProviderIdAndDayOfWeek(providerId, today)
                .map(hours -> hours.isOpenNow(now))
                .orElse(false);
    }

    private DayHours toResponse(BusinessHours hours) {
        return new DayHours(
                hours.getDayOfWeek(),
                DAY_NAMES.get(hours.getDayOfWeek()),
                hours.getOpensAt(),
                hours.getClosesAt(),
                hours.isClosed()
        );
    }
}
