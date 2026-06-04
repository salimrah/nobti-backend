package ma.noba.identity.domain.service;

import ma.noba.identity.domain.model.AuthToken;
import ma.noba.identity.domain.model.User;

import java.util.UUID;

public interface TokenPort {
    AuthToken issueForUser(User user, UUID deviceId);
}
