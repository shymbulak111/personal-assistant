package kz.projem.repository;

import kz.projem.domain.enums.ReminderStatus;
import kz.projem.domain.model.Reminder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    Page<Reminder> findByUserId(Long userId, Pageable pageable);

    Optional<Reminder> findByIdAndUserId(Long id, Long userId);

    // finds reminders that need to be fired
    @Query("SELECT r FROM Reminder r WHERE r.status = 'PENDING' AND r.remindAt <= :now")
    List<Reminder> findDueReminders(@Param("now") LocalDateTime now);

    Page<Reminder> findByUserIdAndStatus(Long userId, ReminderStatus status, Pageable pageable);

    List<Reminder> findAllByUserId(Long userId);
}
