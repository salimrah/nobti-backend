package ma.noba.identity.domain.service;

import ma.noba.identity.domain.model.OtpChannel;

public interface OtpTargetNormalizer {
    String normalize(String target, OtpChannel channel);
}
