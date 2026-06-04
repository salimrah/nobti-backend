package ma.noba.identity.domain.service;

import ma.noba.identity.domain.model.OtpChannel;

public record OtpVerificationResult(String target, OtpChannel channel) {}
