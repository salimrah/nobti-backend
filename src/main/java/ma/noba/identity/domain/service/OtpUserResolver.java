package ma.noba.identity.domain.service;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.model.OtpChannel;
import ma.noba.identity.domain.model.User;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.shared.DomainService;

import java.util.Locale;

@DomainService
@RequiredArgsConstructor
public class OtpUserResolver {

    private final UserRepository users;
    private final UserFactory userFactory;
    private final HashService hashService;

    public User resolve(OtpChannel channel, String target) {
        return switch (channel) {
            case EMAIL -> resolveEmail(target);
            case SMS, WHATSAPP -> resolvePhone(target);
        };
    }

    private User resolveEmail(String email) {
        var normalized = email.toLowerCase(Locale.ROOT);
        var hash = hashService.hash(normalized);
        var user = users.findByEmailHash(hash)
                .orElseGet(() -> userFactory.createWithEmail(email, hash));
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            users.save(user);
        }
        return user;
    }

    private User resolvePhone(String phone) {
        var hash = hashService.hash(phone);
        var user = users.findByPhoneHash(hash)
                .orElseGet(() -> userFactory.createWithPhone(phone, hash));
        if (!user.isPhoneVerified()) {
            user.setPhoneVerified(true);
            users.save(user);
        }
        return user;
    }
}
