package ma.noba.reminder.interfaces.rest;

import jakarta.validation.Valid;
import ma.noba.reminder.application.dto.CreateReminderRequest;
import ma.noba.reminder.application.dto.ReminderResponse;
import ma.noba.reminder.application.dto.UpdateReminderRequest;
import ma.noba.reminder.application.service.ReminderApplicationService;
import ma.noba.shared.domain.user.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reminders")
public class ReminderController {

    private final ReminderApplicationService reminderService;

    public ReminderController(ReminderApplicationService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping
    public ResponseEntity<ReminderResponse> create(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody CreateReminderRequest request) {
        return ResponseEntity.ok(reminderService.create(user.id(), request));
    }

    @GetMapping
    public ResponseEntity<List<ReminderResponse>> getMyReminders(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(reminderService.getByUserId(user.id()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReminderResponse> update(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReminderRequest request) {
        return ResponseEntity.ok(reminderService.update(user.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id) {
        reminderService.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}
