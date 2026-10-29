package ma.noba.media.application.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "storage")
@Getter
@Setter
public class StorageProperties {

    /** local | gcs */
    private String provider = "local";
    private String publicBaseUrl = "/media";
    private long maxUploadBytes = 10 * 1024 * 1024;
    private Local local = new Local();
    private Gcs gcs = new Gcs();

    @Getter
    @Setter
    public static class Local {
        private String directory = "./data/media";
    }

    @Getter
    @Setter
    public static class Gcs {
        private String bucket;
        private String publicBaseUrl;
    }
}
