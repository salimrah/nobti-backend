package ma.noba.packagegift.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.packagegift.application.dto.GiftCardResponse;
import ma.noba.packagegift.application.dto.PurchaseGiftCardRequest;
import ma.noba.packagegift.domain.model.GiftCard;
import ma.noba.packagegift.domain.port.GiftCardRepository;
import ma.noba.shared.domain.exception.BusinessException;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.NotFoundException;
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
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public GiftCardResponse purchaseGiftCard(UUID buyerId, PurchaseGiftCardRequest request) {
        String code = generateCode();
        GiftCard giftCard = new GiftCard(request.getProviderId(), buyerId, request.getAmount(), code);
        giftCard.setRecipientInfo(request.getRecipientName(), request.getMessage());
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
