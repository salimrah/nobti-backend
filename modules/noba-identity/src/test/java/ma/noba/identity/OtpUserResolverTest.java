package ma.noba.identity;

import ma.noba.identity.domain.model.OtpChannel;
import ma.noba.identity.domain.model.User;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.identity.domain.service.HashService;
import ma.noba.identity.domain.service.OtpUserResolver;
import ma.noba.identity.domain.service.UserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static ma.noba.identity.domain.model.OtpChannel.EMAIL;
import static ma.noba.identity.domain.model.OtpChannel.SMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpUserResolverTest {

    @Mock UserRepository users;
    @Mock UserFactory userFactory;
    @Mock HashService hashService;

    OtpUserResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new OtpUserResolver(users, userFactory, hashService);
    }

    @Test
    @DisplayName("Email existing user — returns user, does not save")
    void email_existingUser_returnsUser() {
        var email = "user@example.com";
        var hash = "abc123";
        var existing = User.builder().email(email).emailHash(hash).emailVerified(true).build();
        when(hashService.hash(email.toLowerCase())).thenReturn(hash);
        when(users.findByEmailHash(hash)).thenReturn(Optional.of(existing));

        var result = resolver.resolve(EMAIL, email);

        assertThat(result).isEqualTo(existing);
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("Email new user — creates via factory")
    void email_newUser_createsViaFactory() {
        var email = "new@example.com";
        var hash = "def456";
        var created = User.builder().email(email).emailHash(hash).emailVerified(true).build();
        when(hashService.hash(email.toLowerCase())).thenReturn(hash);
        when(users.findByEmailHash(hash)).thenReturn(Optional.empty());
        when(userFactory.createWithEmail(email, hash)).thenReturn(created);

        var result = resolver.resolve(EMAIL, email);

        assertThat(result).isEqualTo(created);
        verify(userFactory).createWithEmail(email, hash);
    }

    @Test
    @DisplayName("Email existing but not verified — marks verified and saves")
    void email_existingNotVerified_marksVerified() {
        var email = "unverified@example.com";
        var hash = "ghi789";
        var existing = User.builder().email(email).emailHash(hash).emailVerified(false).build();
        when(hashService.hash(email.toLowerCase())).thenReturn(hash);
        when(users.findByEmailHash(hash)).thenReturn(Optional.of(existing));

        var result = resolver.resolve(EMAIL, email);

        assertThat(result.isEmailVerified()).isTrue();
        verify(users).save(existing);
    }

    @Test
    @DisplayName("Phone existing user — returns user, does not save")
    void phone_existingUser_returnsUser() {
        var phone = "+212600000000";
        var hash = "phone123";
        var existing = User.builder().phone(phone).phoneHash(hash).phoneVerified(true).build();
        when(hashService.hash(phone)).thenReturn(hash);
        when(users.findByPhoneHash(hash)).thenReturn(Optional.of(existing));

        var result = resolver.resolve(SMS, phone);

        assertThat(result).isEqualTo(existing);
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("Phone new user — creates via factory")
    void phone_newUser_createsViaFactory() {
        var phone = "+212611111111";
        var hash = "phone456";
        var created = User.builder().phone(phone).phoneHash(hash).phoneVerified(true).build();
        when(hashService.hash(phone)).thenReturn(hash);
        when(users.findByPhoneHash(hash)).thenReturn(Optional.empty());
        when(userFactory.createWithPhone(phone, hash)).thenReturn(created);

        var result = resolver.resolve(SMS, phone);

        assertThat(result).isEqualTo(created);
        verify(userFactory).createWithPhone(phone, hash);
    }

    @Test
    @DisplayName("Phone existing but not verified — marks verified and saves")
    void phone_existingNotVerified_marksVerified() {
        var phone = "+212622222222";
        var hash = "phone789";
        var existing = User.builder().phone(phone).phoneHash(hash).phoneVerified(false).build();
        when(hashService.hash(phone)).thenReturn(hash);
        when(users.findByPhoneHash(hash)).thenReturn(Optional.of(existing));

        var result = resolver.resolve(SMS, phone);

        assertThat(result.isPhoneVerified()).isTrue();
        verify(users).save(existing);
    }

    @Test
    @DisplayName("WhatsApp channel uses same phone resolution as SMS")
    void whatsapp_usesPhoneResolution() {
        var phone = "+212633333333";
        var hash = "wa123";
        var existing = User.builder().phone(phone).phoneHash(hash).phoneVerified(true).build();
        when(hashService.hash(phone)).thenReturn(hash);
        when(users.findByPhoneHash(hash)).thenReturn(Optional.of(existing));

        var result = resolver.resolve(OtpChannel.WHATSAPP, phone);

        assertThat(result).isEqualTo(existing);
    }
}
