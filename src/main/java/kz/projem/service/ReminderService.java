package kz.projem.service;

import kz.projem.config.KafkaConfig;
import kz.projem.domain.enums.ReminderStatus;
import kz.projem.domain.model.Reminder;
import kz.projem.domain.model.User;
import kz.projem.dto.request.ReminderRequest;
import kz.projem.dto.response.ReminderResponse;
import kz.projem.exception.ResourceNotFoundException;
import kz.projem.mapper.ReminderMapper;
import kz.projem.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final ReminderMapper reminderMapper;
    private final Optional<KafkaTemplate<String, Object>> kafkaTemplate;
    private final AuditService auditService;

    @Transactional
    public ReminderResponse create(ReminderRequest request, User user) {
        Reminder reminder = reminderMapper.toEntity(request);
        reminder.setUser(user);
        reminder.setStatus(ReminderStatus.PENDING);
        reminder = reminderRepository.save(reminder);
        auditService.log(user.getId(), user.getEmail(), "CREATE_REMINDER", "REMINDER", reminder.getId(), reminder.getTitle());
        return reminderMapper.toResponse(reminder);
    }

    public Page<ReminderResponse> getAll(User user, Pageable pageable) {
        return reminderRepository.findByUserId(user.getId(), pageable)
                .map(reminderMapper::toResponse);
    }

    public ReminderResponse getById(Long id, User user) {
        return reminderMapper.toResponse(findReminderForUser(id, user.getId()));
    }

    @Transactional
    public ReminderResponse update(Long id, ReminderRequest request, User user) {
        Reminder reminder = findReminderForUser(id, user.getId());

        if (reminder.getStatus() == ReminderStatus.SENT) {
            throw new IllegalArgumentException("Cannot edit a reminder that was already sent");
        }

        reminderMapper.updateFromRequest(request, reminder);
        reminder = reminderRepository.save(reminder);
        return reminderMapper.toResponse(reminder);
    }

    @Transactional
    public void cancel(Long id, User user) {
        Reminder reminder = findReminderForUser(id, user.getId());
        reminder.setStatus(ReminderStatus.CANCELLED);
        reminderRepository.save(reminder);
        auditService.log(user.getId(), user.getEmail(), "CANCEL_REMINDER", "REMINDER", id, null);
    }

    @Transactional
    public void delete(Long id, User user) {
        Reminder reminder = findReminderForUser(id, user.getId());
        reminderRepository.delete(reminder);
    }

    // fires every minute and checks for pending reminders
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void processDueReminders() {
        List<Reminder> due = reminderRepository.findDueReminders(LocalDateTime.now());
        if (due.isEmpty()) return;

        log.info("Processing {} due reminders", due.size());

        for (Reminder r : due) {
            try {
                kafkaTemplate.ifPresent(kt -> kt.send(KafkaConfig.TOPIC_REMINDERS, r.getUser().getEmail(), r));
                r.setStatus(ReminderStatus.SENT);
                r.setSentAt(LocalDateTime.now());

                // schedule next occurrence if repeating
                if (r.getRepeatMinutes() > 0) {
                    Reminder next = Reminder.builder()
                            .title(r.getTitle())
                            .message(r.getMessage())
                            .remindAt(r.getRemindAt().plusMinutes(r.getRepeatMinutes()))
                            .repeatMinutes(r.getRepeatMinutes())
                            .user(r.getUser())
                            .status(ReminderStatus.PENDING)
                            .build();
                    reminderRepository.save(next);
                }

                reminderRepository.save(r);
            } catch (Exception e) {
                log.error("Error processing reminder id={}: {}", r.getId(), e.getMessage());
            }
        }
    }

    private Reminder findReminderForUser(Long id, Long userId) {
        return reminderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", id));
    }
}
