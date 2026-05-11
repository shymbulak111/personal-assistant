package kz.projem.repository;

import kz.projem.domain.model.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Page<ChatSession> findByUserId(Long userId, Pageable pageable);

    Optional<ChatSession> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
