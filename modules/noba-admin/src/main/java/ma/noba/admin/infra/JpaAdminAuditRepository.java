package ma.noba.admin.infra;

import ma.noba.admin.domain.model.AdminAuditEvent;
import ma.noba.admin.domain.port.AdminAuditRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaAdminAuditRepository
        extends JpaRepository<AdminAuditEvent, Long>, AdminAuditRepository {

    @Override
    default AdminAuditEvent persist(AdminAuditEvent event) {
        return save(event);
    }

    @Query("select e from AdminAuditEvent e " +
            "where (:action = '' or e.action = :action) " +
            "and (:targetType = '' or e.targetType = :targetType) " +
            "order by e.createdAt desc, e.id desc")
    Page<AdminAuditEvent> searchBy(@Param("action") String action,
                                   @Param("targetType") String targetType,
                                   Pageable pageable);

    @Override
    default Page<AdminAuditEvent> search(String action, String targetType, Pageable pageable) {
        return searchBy(action == null ? "" : action, targetType == null ? "" : targetType, pageable);
    }
}
