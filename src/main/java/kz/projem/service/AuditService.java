package kz.projem.service;

import kz.projem.config.KafkaConfig;
import kz.projem.domain.model.AuditLog;
import kz.projem.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final Optional<KafkaTemplate<String, Object>> kafkaTemplate;

    @Async
    public void log(Long userId, String email, String action, String entityType, Long entityId, String details) {
        AuditLog entry = AuditLog.builder()
                .userId(userId)
                .userEmail(email)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();

        try {
            auditLogRepository.save(entry);
            kafkaTemplate.ifPresent(kt -> kt.send(KafkaConfig.TOPIC_AUDIT, email, entry));
        } catch (Exception e) {
            log.warn("Failed to save audit log for user={} action={}: {}", email, action, e.getMessage());
        }
    }
}
