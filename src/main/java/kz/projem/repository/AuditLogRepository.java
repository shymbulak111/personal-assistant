package kz.projem.repository;

import kz.projem.domain.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    List<AuditLog> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime from, LocalDateTime to);

    Page<AuditLog> findByUserIdAndAction(Long userId, String action, Pageable pageable);
}
