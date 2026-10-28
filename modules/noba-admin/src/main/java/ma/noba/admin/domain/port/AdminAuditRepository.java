package ma.noba.admin.domain.port;

import ma.noba.admin.domain.model.AdminAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminAuditRepository {

    AdminAuditEvent persist(AdminAuditEvent event);

    Page<AdminAuditEvent> search(String action, String targetType, Pageable pageable);
}
