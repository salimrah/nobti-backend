package ma.noba.notification.interfaces.rest;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.application.dto.CampaignView;
import ma.noba.notification.application.dto.SendCampaignRequest;
import ma.noba.notification.application.service.AdminCampaignService;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.user.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Platform broadcasts from the operations console. */
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBroadcastController {

    private final AdminCampaignService campaigns;

    @GetMapping("/broadcast/preview")
    public ResponseEntity<Map<String, Long>> preview(@RequestParam(defaultValue = "ALL") String audience) {
        return ResponseEntity.ok(Map.of("recipients", campaigns.preview(audience)));
    }

    @PostMapping("/broadcast")
    public ResponseEntity<CampaignView> send(@AuthenticationPrincipal UserPrincipal principal,
                                             @RequestBody SendCampaignRequest request) {
        return ResponseEntity.ok(campaigns.send(principal.id(), principal.email(),
                request.title(), request.body(), request.audience()));
    }

    @GetMapping("/campaigns")
    public ResponseEntity<PagedResponse<CampaignView>> recent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(campaigns.recent(page, size));
    }
}
