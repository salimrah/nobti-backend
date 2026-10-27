package ma.noba.packagegift.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.noba.packagegift.application.dto.GiftCardResponse;
import ma.noba.packagegift.application.dto.PurchaseGiftCardRequest;
import ma.noba.packagegift.application.dto.RedeemGiftCardRequest;
import ma.noba.packagegift.application.service.GiftCardApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gift-cards")
@RequiredArgsConstructor
public class GiftCardController {

    private final GiftCardApplicationService giftCardService;

    @PostMapping("/purchase")
    public ResponseEntity<GiftCardResponse> purchaseGiftCard(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @Valid @RequestBody PurchaseGiftCardRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(giftCardService.purchaseGiftCard(userId, request));
    }

    @PostMapping("/redeem")
    public ResponseEntity<GiftCardResponse> redeemGiftCard(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @Valid @RequestBody RedeemGiftCardRequest request
    ) {
        return ResponseEntity.ok(giftCardService.redeemGiftCard(userId, request.getCode()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<GiftCardResponse>> getMyPurchasedGifts(
        @AuthenticationPrincipal(expression = "id") UUID userId
    ) {
        return ResponseEntity.ok(giftCardService.getMyPurchasedGifts(userId));
    }
}
