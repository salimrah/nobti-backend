package ma.noba.identity.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import ma.noba.identity.domain.model.SecurityLog;
import ma.noba.identity.domain.repository.SecurityLogRepository;
import ma.noba.shared.DomainService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

@DomainService
@Transactional
public class AuditService {
    private final SecurityLogRepository repo;
    private static final ObjectMapper mapper = new ObjectMapper();

    public AuditService(SecurityLogRepository repo) {
        this.repo = repo;
    }

    public void record(UUID userId, String eventType, String ip, String userAgent) {
        record(userId, eventType, ip, userAgent, null);
    }

    public void record(UUID userId, String eventType, String ip, String userAgent, Map<String, Object> metadata) {
        if (ip == null || userAgent == null) {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                if (ip == null) ip = req.getRemoteAddr();
                if (userAgent == null) userAgent = req.getHeader("User-Agent");
            }
        }
        repo.save(SecurityLog.builder()
                .userId(userId)
                .eventType(eventType)
                .ipAddress(ip)
                .userAgent(userAgent)
                .payload(metadata == null ? "{}" : mapper.valueToTree(metadata).toString())
                .build());
    }
}
