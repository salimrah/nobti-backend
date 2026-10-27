package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.model.OtpChannel;
import ma.noba.identity.domain.service.OtpTargetNormalizer;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class GoogleOtpTargetNormalizer implements OtpTargetNormalizer {

    @Override
    public String normalize(String target, OtpChannel channel) {

        if (channel == OtpChannel.EMAIL)
            return target.trim().toLowerCase(Locale.ROOT);

        try {
            var util = com.google.i18n.phonenumbers.PhoneNumberUtil.getInstance();
            var num = util.parse(target, null);
            return util.format(num, com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid phone");
        }

    }
}

