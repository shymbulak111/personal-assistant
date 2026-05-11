package kz.projem.repository;

import kz.projem.domain.enums.TaskPriority;
import kz.projem.domain.enums.TaskStatus;
import kz.projem.domain.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByUserId(Long userId, Pageable pageable);

    Page<Task> findByUserIdAndStatus(Long userId, TaskStatus status, Pageable pageable);

    Page<Task> findByUserIdAndPriority(Long userId, TaskPriority priority, Pageable pageable);

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    List<Task> findByUserIdAndDueDateBefore(Long userId, LocalDate date);

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Task> searchTasks(@Param("userId") Long userId,
                           @Param("query") String query,
                           Pageable pageable);

    long countByUserIdAndStatus(Long userId, TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND t.dueDate = :date")
    List<Task> findByUserIdAndDueDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND t.status IN ('TODO','IN_PROGRESS') ORDER BY t.priority DESC, t.dueDate ASC NULLS LAST")
    List<Task> findActiveTasks(@Param("userId") Long userId, Pageable pageable);

    List<Task> findAllByUserId(Long userId);
}
