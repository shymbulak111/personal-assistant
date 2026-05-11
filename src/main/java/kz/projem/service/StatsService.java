package kz.projem.service;

import kz.projem.domain.enums.ReminderStatus;
import kz.projem.domain.enums.TaskStatus;
import kz.projem.domain.model.User;
import kz.projem.dto.response.StatsResponse;
import kz.projem.repository.ChatSessionRepository;
import kz.projem.repository.NoteRepository;
import kz.projem.repository.ReminderRepository;
import kz.projem.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final TaskRepository taskRepository;
    private final NoteRepository noteRepository;
    private final ReminderRepository reminderRepository;
    private final ChatSessionRepository chatSessionRepository;

    @Cacheable(value = "stats", key = "#user.id")
    public StatsResponse getStats(User user) {
        long totalTasks = taskRepository.findByUserId(user.getId(), Pageable.unpaged()).getTotalElements();
        long completedTasks = taskRepository.countByUserIdAndStatus(user.getId(), TaskStatus.DONE);
        long pendingTasks = taskRepository.countByUserIdAndStatus(user.getId(), TaskStatus.TODO);

        long overdue = taskRepository.findByUserIdAndDueDateBefore(user.getId(), LocalDate.now())
                .stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE && t.getStatus() != TaskStatus.CANCELLED)
                .count();

        long totalNotes = noteRepository.findByUserIdAndArchived(user.getId(), false, Pageable.unpaged()).getTotalElements();
        long pinned = noteRepository.findByUserIdAndPinnedTrueAndArchivedFalse(user.getId()).size();

        long pendingReminders = reminderRepository.findByUserIdAndStatus(
                user.getId(), ReminderStatus.PENDING, Pageable.unpaged()
        ).getTotalElements();

        long chatSessions = chatSessionRepository.findByUserId(user.getId(), Pageable.unpaged()).getTotalElements();

        return StatsResponse.builder()
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .pendingTasks(pendingTasks)
                .overdueTasks(overdue)
                .totalNotes(totalNotes)
                .pinnedNotes(pinned)
                .pendingReminders(pendingReminders)
                .totalChatSessions(chatSessions)
                .aiRequestsUsed(user.getAiRequestsCount())
                .aiRequestsLimit(user.getAiRequestsLimit())
                .build();
    }
}
