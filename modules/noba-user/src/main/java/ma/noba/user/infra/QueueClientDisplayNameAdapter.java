package ma.noba.user.infra;

import lombok.RequiredArgsConstructor;
import ma.noba.queue.domain.port.service.ClientDisplayNamePort;
import ma.noba.user.domain.model.UserProfile;
import ma.noba.user.domain.port.UserProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class QueueClientDisplayNameAdapter implements ClientDisplayNamePort {

    private final UserProfileRepository userProfileRepository;

    @Override
    public Map<UUID, String> findDisplayNames(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> names = new HashMap<>();
        for (UserProfile profile : userProfileRepository.findByUserIdIn(userIds)) {
            if (profile.getUserId() == null) {
                continue;
            }
            String name = format(profile);
            if (name != null) {
                names.put(profile.getUserId(), name);
            }
        }
        return names;
    }

    private static String format(UserProfile profile) {
        if (profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
            return profile.getDisplayName().trim();
        }
        String first = profile.getFirstName() != null ? profile.getFirstName().trim() : "";
        String last = profile.getLastName() != null ? profile.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? null : full;
    }
}
