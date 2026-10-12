package ma.noba.referral.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.noba.referral.application.dto.*;
import ma.noba.referral.application.service.ReferralApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralApplicationService referralService;

    @PostMapping("/create")
    public ResponseEntity<CreateReferralResponse> createReferral(@AuthenticationPrincipal(expression = "id") UUID userId) {
        return ResponseEntity.ok(referralService.createReferral(userId));
    }

    @PostMapping("/claim")
    public ResponseEntity<ReferralResponse> claimReferral(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @Valid @RequestBody ClaimReferralRequest request
    ) {
        return ResponseEntity.ok(referralService.claimReferral(userId, request.getCode()));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ReferralResponse>> getMyReferrals(@AuthenticationPrincipal(expression = "id") UUID userId) {
        return ResponseEntity.ok(referralService.getMyReferrals(userId));
    }
}
