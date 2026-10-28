package ma.noba.payment;

import ma.noba.payment.domain.model.PaymentTransaction;
import ma.noba.payment.infra.ManualPaymentGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ManualPaymentGatewayTest {

    private final ManualPaymentGateway gateway = new ManualPaymentGateway();
    private final UUID userId = UUID.randomUUID();
    private final UUID providerId = UUID.randomUUID();

    private PaymentTransaction transaction() {
        return new PaymentTransaction(userId, providerId, null, "MAD", new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Manual gateway records a local reference instead of calling a provider")
    void createPaymentIntentUsesLocalReference() {
        PaymentTransaction transaction = transaction();

        String token = gateway.createPaymentIntent(transaction, null, null);

        assertThat(token).startsWith("manual_");
        assertThat(transaction.getStripePaymentIntentId()).startsWith("manual_");
    }

    @Test
    @DisplayName("Manual payouts are admin-managed and always enabled")
    void payoutsAreAdminManaged() {
        assertThat(gateway.accountPayoutsEnabled("anything")).isTrue();
        assertThat(gateway.createAccountLink(providerId, "return", "refresh")).isEqualTo("refresh");
    }

    @Test
    @DisplayName("Manual transfers get a local id so they can be reversed")
    void transfersUseLocalIds() {
        String transferId = gateway.transferToProvider(
                "charge_1", "account_1", new BigDecimal("95.00"), "MAD");

        assertThat(transferId).startsWith("manual_transfer_");
    }
}
