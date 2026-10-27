package ma.noba.app.packagegift;

import lombok.RequiredArgsConstructor;
import ma.noba.packagegift.domain.port.GiftCardPaymentPort;
import ma.noba.payment.domain.model.PaymentStatus;
import ma.noba.payment.domain.port.PaymentTransactionRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GiftCardPaymentAdapter implements GiftCardPaymentPort {

    private final PaymentTransactionRepository payments;

    @Override
    public Optional<VerifiedPayment> findVerifiedPayment(UUID paymentTransactionId) {
        return payments.findById(paymentTransactionId)
                .map(tx -> new VerifiedPayment(
                        tx.getId(),
                        tx.getUserId(),
                        tx.getProviderId(),
                        tx.getAmount(),
                        tx.getStatus() == PaymentStatus.SUCCEEDED));
    }
}
