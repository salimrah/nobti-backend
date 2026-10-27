package ma.noba.provider.interfaces.rest.rating;

import ma.noba.provider.application.service.ProviderApplicationService;
import ma.noba.shared.domain.user.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/providers/{providerId}")
public class ProviderRatingController {

    private final ProviderApplicationService providerService;

    public ProviderRatingController(ProviderApplicationService providerService) {
        this.providerService = providerService;
    }

    @PostMapping("/ratings")
    public ResponseEntity<Void> rateProvider(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID providerId,
            @RequestParam int value,
            @RequestParam(required = false) String comment) {
        providerService.rateProvider(user.id(), providerId, value, comment);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ratings/average")
    public ResponseEntity<Double> getAverageRating(@PathVariable UUID providerId) {
        return ResponseEntity.ok(providerService.getAverageRating(providerId));
    }

    @PostMapping("/favorite")
    public ResponseEntity<Void> toggleFavorite(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID providerId,
            @RequestParam boolean favorite) {
        providerService.toggleFavorite(user.id(), providerId, favorite);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/favorite")
    public ResponseEntity<Boolean> isFavorite(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID providerId) {
        return ResponseEntity.ok(providerService.isFavorite(user.id(), providerId));
    }
}
