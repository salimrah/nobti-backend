package ma.noba.identity.infrastructure.listener;

import ma.noba.identity.domain.event.OtpFailedEvent;
import ma.noba.identity.domain.event.OtpInitiatedEvent;
import ma.noba.identity.domain.event.OtpVerifiedEvent;
import ma.noba.identity.domain.event.UserAuthenticatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class IdentityEventListener {

    private static final Logger log = LoggerFactory.getLogger(IdentityEventListener.class);

    @TransactionalEventListener
    public void onUserAuthenticated(UserAuthenticatedEvent event) {
        log.info("User authenticated: userId={}, method={}", event.getUserId(), event.getMethod());
    }

    @TransactionalEventListener
    public void onOtpInitiated(OtpInitiatedEvent event) {
        log.debug("OTP initiated: {}", event.eventType());
    }

    @TransactionalEventListener
    public void onOtpVerified(OtpVerifiedEvent event) {
        log.debug("OTP verified: {}", event.eventType());
    }

    @TransactionalEventListener
    public void onOtpFailed(OtpFailedEvent event) {
        log.warn("OTP failed: {}", event.eventType());
    }
}
