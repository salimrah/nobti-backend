package ma.noba.payment.domain.port;

import ma.noba.payment.domain.model.PaymentWebhookEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface PaymentWebhookEventRepository {

    PaymentWebhookEvent save(PaymentWebhookEvent event);

    Optional<PaymentWebhookEvent> findById(UUID id);

    Optional<PaymentWebhookEvent> findByProviderAndEventId(String provider, String eventId);

    Page<PaymentWebhookEvent> findAllByOrderByReceivedAtDesc(Pageable pageable);

    Page<PaymentWebhookEvent> findByStatusOrderByReceivedAtDesc(String status, Pageable pageable);
}
