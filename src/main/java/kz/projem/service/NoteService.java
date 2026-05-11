package kz.projem.service;

import kz.projem.domain.model.Note;
import kz.projem.domain.model.User;
import kz.projem.dto.request.NoteRequest;
import kz.projem.dto.response.NoteResponse;
import kz.projem.exception.ResourceNotFoundException;
import kz.projem.mapper.NoteMapper;
import kz.projem.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteMapper noteMapper;
    private final AuditService auditService;

    @Transactional
    public NoteResponse create(NoteRequest request, User user) {
        Note note = noteMapper.toEntity(request);
        note.setUser(user);
        note = noteRepository.save(note);
        auditService.log(user.getId(), user.getEmail(), "CREATE_NOTE", "NOTE", note.getId(), note.getTitle());
        return noteMapper.toResponse(note);
    }

    public Page<NoteResponse> getAll(User user, boolean includeArchived, Pageable pageable) {
        return noteRepository.findByUserIdAndArchived(user.getId(), includeArchived, pageable)
                .map(noteMapper::toResponse);
    }

    public List<NoteResponse> getPinned(User user) {
        return noteRepository.findByUserIdAndPinnedTrueAndArchivedFalse(user.getId())
                .stream()
                .map(noteMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Page<NoteResponse> search(User user, String query, Pageable pageable) {
        return noteRepository.search(user.getId(), query, pageable)
                .map(noteMapper::toResponse);
    }

    public NoteResponse getById(Long id, User user) {
        return noteMapper.toResponse(findNoteForUser(id, user.getId()));
    }

    @Transactional
    public NoteResponse update(Long id, NoteRequest request, User user) {
        Note note = findNoteForUser(id, user.getId());
        noteMapper.updateFromRequest(request, note);
        note = noteRepository.save(note);
        auditService.log(user.getId(), user.getEmail(), "UPDATE_NOTE", "NOTE", note.getId(), note.getTitle());
        return noteMapper.toResponse(note);
    }

    @Transactional
    public void delete(Long id, User user) {
        Note note = findNoteForUser(id, user.getId());
        noteRepository.delete(note);
        auditService.log(user.getId(), user.getEmail(), "DELETE_NOTE", "NOTE", id, null);
    }

    @Transactional
    public NoteResponse togglePin(Long id, User user) {
        Note note = findNoteForUser(id, user.getId());
        note.setPinned(!note.isPinned());
        return noteMapper.toResponse(noteRepository.save(note));
    }

    @Transactional
    public NoteResponse toggleArchive(Long id, User user) {
        Note note = findNoteForUser(id, user.getId());
        note.setArchived(!note.isArchived());
        if (note.isArchived()) note.setPinned(false);
        return noteMapper.toResponse(noteRepository.save(note));
    }

    private Note findNoteForUser(Long noteId, Long userId) {
        return noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Note", noteId));
    }
}
