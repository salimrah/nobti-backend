package ma.noba.provider.interfaces.rest;

import ma.noba.provider.application.dto.ProviderDashboardResponse;
import ma.noba.provider.application.service.ProviderDashboardService;
import ma.noba.shared.domain.user.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/providers/self/dashboard")
public class ProviderDashboardController {

    private final ProviderDashboardService dashboardService;

    public ProviderDashboardController(ProviderDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<ProviderDashboardResponse> getDashboard(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(dashboardService.getDashboard(user.id()));
    }
}
