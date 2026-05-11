package kz.projem.service;

import kz.projem.domain.model.User;
import kz.projem.dto.response.UserDataExportResponse;
import kz.projem.mapper.NoteMapper;
import kz.projem.mapper.ReminderMapper;
import kz.projem.mapper.TaskMapper;
import kz.projem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GdprService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final NoteRepository noteRepository;
    private final ReminderRepository reminderRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final TaskMapper taskMapper;
    private final NoteMapper noteMapper;
    private final ReminderMapper reminderMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public UserDataExportResponse exportUserData(User user) {
        auditService.log(user.getId(), user.getEmail(), "GDPR_EXPORT", "USER", user.getId(), null);

        return UserDataExportResponse.builder()
                .email(user.getEmail())
                .fullName(user.getFullName())
                .accountCreated(user.getCreatedAt())
                .tasks(taskRepository.findAllByUserId(user.getId())
                        .stream().map(taskMapper::toResponse).toList())
                .notes(noteRepository.findAllByUserId(user.getId())
                        .stream().map(noteMapper::toResponse).toList())
                .reminders(reminderRepository.findAllByUserId(user.getId())
                        .stream().map(reminderMapper::toResponse).toList())
                .totalChatSessions((int) chatSessionRepository.countByUserId(user.getId()))
                .exportedAt(LocalDateTime.now().toString())
                .build();
    }

    @Transactional
    public void deleteAccount(User user) {
        auditService.log(user.getId(), user.getEmail(), "GDPR_DELETE_ACCOUNT", "USER", user.getId(), "User-initiated account deletion");
        userRepository.delete(user);
    }
}
