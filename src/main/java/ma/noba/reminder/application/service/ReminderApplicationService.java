package ma.noba.reminder.application.service;

import ma.noba.reminder.application.dto.CreateReminderRequest;
import ma.noba.reminder.application.dto.ReminderResponse;
import ma.noba.reminder.application.dto.UpdateReminderRequest;
import ma.noba.reminder.domain.model.ServiceReminder;
import ma.noba.reminder.domain.port.ServiceReminderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ReminderApplicationService {

    private final ServiceReminderRepository reminderRepository;

    public ReminderApplicationService(ServiceReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    public ReminderResponse create(UUID userId, CreateReminderRequest request) {
        ServiceReminder reminder = new ServiceReminder(
            userId,
            request.providerId(),
            request.serviceId(),
            request.providerName(),
            request.serviceName(),
            request.intervalDays()
        );
        reminder = reminderRepository.save(reminder);
        return toResponse(reminder);
    }

    @Transactional(readOnly = true)
    public List<ReminderResponse> getByUserId(UUID userId) {
        return reminderRepository.findByUserId(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    public ReminderResponse update(UUID userId, UUID reminderId, UpdateReminderRequest request) {
        ServiceReminder reminder = findOwned(userId, reminderId);
        if (request.intervalDays() != null) {
            reminder.updateInterval(request.intervalDays());
        }
        if (request.active() != null) {
            reminder.setActive(request.active());
        }
        reminder = reminderRepository.save(reminder);
        return toResponse(reminder);
    }

    public void delete(UUID userId, UUID reminderId) {
        ServiceReminder reminder = findOwned(userId, reminderId);
        reminderRepository.delete(reminder);
    }

    private ServiceReminder findOwned(UUID userId, UUID reminderId) {
        ServiceReminder reminder = reminderRepository.findById(reminderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reminder not found"));
        if (!reminder.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return reminder;
    }

    private ReminderResponse toResponse(ServiceReminder r) {
        return new ReminderResponse(
            r.getId(),
            r.getProviderId(),
            r.getServiceId(),
            r.getProviderName(),
            r.getServiceName(),
            r.getIntervalDays(),
            r.getNextReminderAt(),
            r.isActive(),
            r.getCreatedAt()
        );
    }
}
