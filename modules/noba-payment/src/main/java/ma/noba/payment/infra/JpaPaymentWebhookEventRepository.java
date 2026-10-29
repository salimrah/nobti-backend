package ma.noba.payment.infra;

import ma.noba.payment.domain.model.PaymentWebhookEvent;
import ma.noba.payment.domain.port.PaymentWebhookEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaPaymentWebhookEventRepository
        extends JpaRepository<PaymentWebhookEvent, UUID>, PaymentWebhookEventRepository {

    @Override
    Optional<PaymentWebhookEvent> findByProviderAndEventId(String provider, String eventId);

    @Override
    Page<PaymentWebhookEvent> findAllByOrderByReceivedAtDesc(Pageable pageable);

    @Override
    Page<PaymentWebhookEvent> findByStatusOrderByReceivedAtDesc(String status, Pageable pageable);
}
