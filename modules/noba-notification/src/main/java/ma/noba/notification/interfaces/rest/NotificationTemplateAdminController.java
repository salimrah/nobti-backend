package ma.noba.notification.interfaces.rest;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.application.dto.CreateTemplateRequest;
import ma.noba.notification.application.dto.SetTemplateActiveRequest;
import ma.noba.notification.application.dto.TemplateResponse;
import ma.noba.notification.application.service.NotificationTemplateAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Admin workflow for notification templates: list versions, publish a new version and switch the
 * active one. Rendering picks the highest active version for the key + locale.
 */
@RestController
@RequestMapping("/api/v1/notifications/templates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NotificationTemplateAdminController {

    private final NotificationTemplateAdminService templates;

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> list(
            @RequestParam(required = false) String key,
            @RequestParam(required = false) String locale) {
        return ResponseEntity.ok(templates.list(key, locale));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> get(@PathVariable UUID id) {
        return templates.get(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> create(@RequestBody CreateTemplateRequest request) {
        TemplateResponse created = templates.create(request);
        return ResponseEntity.created(URI.create("/api/v1/notifications/templates/" + created.id()))
                .body(created);
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<TemplateResponse> setActive(@PathVariable UUID id,
                                                      @RequestBody SetTemplateActiveRequest request) {
        return ResponseEntity.ok(templates.setActive(id, request.active()));
    }
}
