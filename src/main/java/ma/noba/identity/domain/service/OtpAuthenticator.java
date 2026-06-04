package ma.noba.identity.domain.service;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.application.OtpVerifyRequest;
import ma.noba.identity.domain.event.UserAuthenticatedEvent;
import ma.noba.identity.domain.model.AuthMethod;
import ma.noba.identity.application.AuthToken;
import ma.noba.identity.domain.model.User;
import ma.noba.shared.DomainService;
import ma.noba.shared.domain.event.EventBus;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@RequiredArgsConstructor
public class OtpAuthenticator implements Authenticator<OtpVerifyRequest, AuthToken> {
    private final OtpService otpService;
    private final TokenPort tokens;
    private final AuditService auditService;
    private final EventBus eventBus;
    private final OtpUserResolver otpUserResolver;

    @Override
    public boolean supports(AuthMethod method) {
        return method == AuthMethod.PHONE_OTP || method == AuthMethod.EMAIL_OTP;
    }

    @Override
    @Transactional
    public AuthToken authenticate(OtpVerifyRequest req) {
        var result = otpService.verify(req.challengeId(), req.code());
        User user = otpUserResolver.resolve(result.channel(), result.target());
        auditService.record(user.getId(), "OTP_LOGIN:" + result.channel().name(), null, null);
        eventBus.publish(new UserAuthenticatedEvent(user.getId().toString(), "otp:" + result.channel().name().toLowerCase()));
        return tokens.issueForUser(user, req.deviceId());
    }
}
