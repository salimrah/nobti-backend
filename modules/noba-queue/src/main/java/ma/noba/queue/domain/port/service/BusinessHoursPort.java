package ma.noba.queue.domain.port.service;

import java.util.UUID;

public interface BusinessHoursPort {

    boolean isOpenNow(UUID providerId);
}
