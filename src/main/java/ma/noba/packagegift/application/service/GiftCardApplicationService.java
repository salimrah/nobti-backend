package ma.noba.packagegift.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.packagegift.application.dto.GiftCardResponse;
import ma.noba.packagegift.application.dto.PurchaseGiftCardRequest;
import ma.noba.packagegift.domain.model.GiftCard;
import ma.noba.packagegift.domain.port.GiftCardRepository;
import ma.noba.payment.domain.model.PaymentStatus;
import ma.noba.payment.domain.model.PaymentTransaction;
import ma.noba.payment.domain.port.PaymentTransactionRepository;
import ma.noba.shared.domain.exception.BusinessException;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.NotFoundException;
import ma.noba.shared.domain.exception.ValidationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GiftCardApplicationService {

    private final GiftCardRepository giftCardRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public GiftCardResponse purchaseGiftCard(UUID buyerId, PurchaseGiftCardRequest request) {
        // Issuance is gated on a verified, owned, matching SUCCEEDED payment. Without this, a
        // gift card could be minted from a client-supplied amount with no payment at all.
        PaymentTransaction payment = paymentTransactionRepository.findById(request.getPaymentTransactionId())
            .orElseThrow(() -> new ValidationException(ErrorCode.VALIDATION_ERROR, "Payment transaction not found"));

        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR, "Gift card purchase requires a successful payment");
        }
        if (!payment.getUserId().equals(buyerId)) {
            throw new AccessDeniedException("Payment does not belong to the buyer");
        }
        if (!payment.getProviderId().equals(request.getProviderId())) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR, "Payment provider does not match the gift card provider");
        }
        if (payment.getAmount() == null || payment.getAmount().compareTo(request.getAmount()) != 0) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR, "Payment amount does not match the gift card amount");
        }
        if (giftCardRepository.existsByPaymentTransactionId(payment.getId())) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR, "This payment has already been used for a gift card");
        }

        String code = generateCode();
        GiftCard giftCard = new GiftCard(request.getProviderId(), buyerId, request.getAmount(), code);
        giftCard.setRecipientInfo(request.getRecipientName(), request.getMessage());
        giftCard.setPaymentTransactionId(payment.getId());
        giftCard = giftCardRepository.save(giftCard);
        return toResponse(giftCard);
    }

    @Transactional
    public GiftCardResponse redeemGiftCard(UUID redeemerId, String code) {
        GiftCard giftCard = giftCardRepository.findByCode(code)
            .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "Gift card not found"));

        if (giftCard.isRedeemed()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Gift card already redeemed");
        }

        giftCard.redeem(redeemerId);
        giftCard = giftCardRepository.save(giftCard);
        return toResponse(giftCard);
    }

    public List<GiftCardResponse> getMyPurchasedGifts(UUID buyerId) {
        return giftCardRepository.findByBuyerId(buyerId).stream()
            .map(this::toResponse)
            .toList();
    }

    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder("NOBA-");
        for (int i = 0; i < 8; i++) {
            if (i == 4) code.append('-');
            code.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return code.toString();
    }

    private GiftCardResponse toResponse(GiftCard gc) {
        return new GiftCardResponse(
            gc.getId().toString(), gc.getProviderId().toString(),
            gc.getBuyerId().toString(),
            gc.getRedeemerId() != null ? gc.getRedeemerId().toString() : null,
            gc.getRecipientName(), gc.getAmount(), gc.getCode(),
            gc.getMessage(), gc.isRedeemed(),
            gc.getRedeemedAt() != null ? gc.getRedeemedAt().toString() : null,
            gc.getCreatedAt().toString());
    }
}
