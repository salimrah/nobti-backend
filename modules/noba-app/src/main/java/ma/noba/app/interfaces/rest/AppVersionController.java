package ma.noba.app.interfaces.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Lets the mobile app detect outdated builds without downloading an update first. */
@RestController
@RequestMapping("/api/v1/app")
public class AppVersionController {

    @Value("${app.version.latest:1.0.0}")
    private String latest;

    @Value("${app.version.min-supported:1.0.0}")
    private String minSupported;

    @Value("${app.version.message:}")
    private String message;

    @Value("${app.version.ios-url:}")
    private String iosUrl;

    @Value("${app.version.android-url:}")
    private String androidUrl;

    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> version() {
        return ResponseEntity.ok(Map.of(
                "latest", latest,
                "minSupported", minSupported,
                "message", message,
                "iosUrl", iosUrl,
                "androidUrl", androidUrl));
    }
}
