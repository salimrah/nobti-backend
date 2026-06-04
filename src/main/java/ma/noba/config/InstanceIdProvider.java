package ma.noba.config;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InstanceIdProvider {
    private final String instanceId;

    public InstanceIdProvider() {
        this.instanceId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public String getInstanceId() {
        return instanceId;
    }
}
