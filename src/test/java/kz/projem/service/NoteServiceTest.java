package kz.projem.service;

import kz.projem.domain.model.Note;
import kz.projem.domain.model.User;
import kz.projem.dto.request.NoteRequest;
import kz.projem.dto.response.NoteResponse;
import kz.projem.exception.ResourceNotFoundException;
import kz.projem.mapper.NoteMapper;
import kz.projem.repository.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class NoteServiceTest {

    @Mock NoteRepository noteRepository;
    @Mock NoteMapper noteMapper;
    @Mock AuditService auditService;

    @InjectMocks
    NoteService noteService;

    private User user;
    private Note note;

    @BeforeEach
    void setup() {
        user = User.builder().id(1L).email("user@test.com").build();
        note = Note.builder()
                .id(1L)
                .title("My note")
                .content("Some content")
                .pinned(false)
                .archived(false)
                .user(user)
                .build();
    }

    @Test
    void create_savesNoteAndReturnsResponse() {
        NoteRequest req = new NoteRequest();
        req.setTitle("My note");
        req.setContent("Some content");

        NoteResponse expected = new NoteResponse();
        expected.setId(1L);
        expected.setTitle("My note");

        when(noteMapper.toEntity(req)).thenReturn(note);
        when(noteRepository.save(any())).thenReturn(note);
        when(noteMapper.toResponse(note)).thenReturn(expected);

        NoteResponse result = noteService.create(req, user);

        assertThat(result.getTitle()).isEqualTo("My note");
        verify(noteRepository).save(note);
    }

    @Test
    void togglePin_flipsPinnedStatus() {
        when(noteRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NoteResponse resp = new NoteResponse();
        resp.setPinned(true);
        when(noteMapper.toResponse(any())).thenReturn(resp);

        noteService.togglePin(1L, user);

        assertThat(note.isPinned()).isTrue();
    }

    @Test
    void toggleArchive_archivedNoteUnpinsIt() {
        note.setPinned(true);
        when(noteRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(noteMapper.toResponse(any())).thenReturn(new NoteResponse());

        noteService.toggleArchive(1L, user);

        assertThat(note.isArchived()).isTrue();
        assertThat(note.isPinned()).isFalse();
    }

    @Test
    void getById_notFound_throws() {
        when(noteRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getById(999L, user))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
