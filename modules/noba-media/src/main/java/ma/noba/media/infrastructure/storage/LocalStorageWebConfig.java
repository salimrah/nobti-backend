package ma.noba.media.infrastructure.storage;

import ma.noba.media.application.storage.StorageProperties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/** Serves locally stored public uploads at /media/** (Caddy proxies them in production). */
@Configuration
@ConditionalOnProperty(name = "storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageWebConfig implements WebMvcConfigurer {

    private final StorageProperties props;

    public LocalStorageWebConfig(StorageProperties props) {
        this.props = props;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path publicDir = Path.of(props.getLocal().getDirectory()).toAbsolutePath().normalize().resolve("public");
        registry.addResourceHandler("/media/**")
                .addResourceLocations(publicDir.toUri().toString());
    }
}
