package ma.noba.payment.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.noba.payment.application.dto.*;
import ma.noba.payment.application.service.PaymentApplicationService;
import ma.noba.payment.application.service.RevenueService;
import ma.noba.payment.domain.model.PaymentTransaction;
import ma.noba.payment.domain.model.Payout;
import ma.noba.payment.domain.port.ProviderLookupPort;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentApplicationService paymentService;
    private final ProviderLookupPort providerLookupPort;
    private final RevenueService revenueService;

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> createCheckout(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @Valid @RequestBody CreateCheckoutRequest request
    ) {
        CheckoutResponse response = paymentService.createCheckout(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @PathVariable UUID id
    ) {
        PaymentTransaction tx = paymentService.getTransaction(id);
        UUID callerProviderId = providerLookupPort.findProviderIdByUserId(userId).orElse(null);
        if (!tx.getUserId().equals(userId) && !tx.getProviderId().equals(callerProviderId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(toTransactionResponse(tx));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponse>> getMyTransactions(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<PaymentTransaction> transactions = paymentService.getUserTransactions(userId, page, size);
        return ResponseEntity.ok(transactions.map(PaymentController::toTransactionResponse));
    }

    @GetMapping("/providers/{providerId}/transactions")
    public ResponseEntity<Page<TransactionResponse>> getProviderTransactions(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @PathVariable UUID providerId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        if (!providerId.equals(providerLookupPort.findProviderIdByUserId(userId).orElse(null))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Page<PaymentTransaction> transactions = paymentService.getProviderTransactions(providerId, page, size);
        return ResponseEntity.ok(transactions.map(PaymentController::toTransactionResponse));
    }

    @PostMapping("/providers/self/payout-settings")
    public ResponseEntity<PayoutSettingsResponse> getOrCreatePayoutSettings(
        @AuthenticationPrincipal(expression = "id") UUID userId
    ) {
        UUID providerId = getProviderIdForUser(userId);
        PayoutSettingsResponse response = paymentService.getOrCreatePayoutSettings(providerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/providers/self/payout-settings/account")
    public ResponseEntity<Void> updateStripeAccount(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @RequestParam String stripeAccountId
    ) {
        UUID providerId = getProviderIdForUser(userId);
        paymentService.updateStripeAccount(providerId, stripeAccountId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/providers/self/payouts")
    public ResponseEntity<Page<PayoutResponse>> getMyPayouts(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        UUID providerId = getProviderIdForUser(userId);
        Page<Payout> payouts = paymentService.getProviderPayouts(providerId, page, size);
        return ResponseEntity.ok(payouts.map(p -> new PayoutResponse(
            p.getId().toString(),
            p.getProviderId().toString(),
            p.getAmount(),
            p.getCurrency(),
            p.getStatus().name(),
            p.getCreatedAt(),
            p.getPaidAt()
        )));
    }

    @PostMapping("/providers/{providerId}/refund")
    public ResponseEntity<Void> refund(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @PathVariable UUID providerId,
        @RequestParam UUID transactionId
    ) {
        // Only the provider that owns the transaction may refund it (previously any authenticated
        // caller could refund any transaction).
        if (!providerId.equals(providerLookupPort.findProviderIdByUserId(userId).orElse(null))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        PaymentTransaction tx = paymentService.getTransaction(transactionId);
        if (!tx.getProviderId().equals(providerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        paymentService.processRefund(transactionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/providers/self/revenue")
    public ResponseEntity<RevenueSummaryResponse> getRevenue(@AuthenticationPrincipal(expression = "id") UUID userId) {
        UUID providerId = getProviderIdForUser(userId);
        return ResponseEntity.ok(revenueService.getRevenueSummary(providerId));
    }

    private static TransactionResponse toTransactionResponse(PaymentTransaction tx) {
        return new TransactionResponse(
            tx.getId().toString(),
            tx.getUserId().toString(),
            tx.getProviderId().toString(),
            tx.getServiceId() != null ? tx.getServiceId().toString() : null,
            tx.getCurrency(),
            tx.getAmount(),
            tx.getFee(),
            tx.getNetAmount(),
            tx.getStatus().name(),
            tx.getStripePaymentIntentId(),
            tx.getFailureMessage(),
            tx.getCreatedAt(),
            tx.getCompletedAt()
        );
    }

    private UUID getProviderIdForUser(UUID userId) {
        return providerLookupPort.findProviderIdByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No provider profile found for user"));
    }
}
