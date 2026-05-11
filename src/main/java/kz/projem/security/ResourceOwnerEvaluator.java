package kz.projem.security;

import kz.projem.repository.NoteRepository;
import kz.projem.repository.ReminderRepository;
import kz.projem.repository.TaskRepository;
import kz.projem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * ABAC — Attribute-Based Access Control.
 * Used in @PreAuthorize("@ownerCheck.isXxxOwner(#id, authentication)")
 * to verify that the authenticated user owns the target resource.
 */
@Component("ownerCheck")
@RequiredArgsConstructor
public class ResourceOwnerEvaluator {

    private final TaskRepository taskRepository;
    private final NoteRepository noteRepository;
    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;

    public boolean isTaskOwner(Long taskId, Authentication auth) {
        return owns(taskRepository.findById(taskId).map(t -> t.getUser().getId()), auth);
    }

    public boolean isNoteOwner(Long noteId, Authentication auth) {
        return owns(noteRepository.findById(noteId).map(n -> n.getUser().getId()), auth);
    }

    public boolean isReminderOwner(Long reminderId, Authentication auth) {
        return owns(reminderRepository.findById(reminderId).map(r -> r.getUser().getId()), auth);
    }

    private boolean owns(Optional<Long> ownerIdOpt, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;
        return ownerIdOpt
                .flatMap(ownerId -> userRepository.findByEmail(auth.getName())
                        .map(u -> u.getId().equals(ownerId)))
                .orElse(false);
    }
}
