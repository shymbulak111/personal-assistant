package kz.projem.service;

import kz.projem.domain.enums.TaskPriority;
import kz.projem.domain.enums.TaskStatus;
import kz.projem.domain.model.Task;
import kz.projem.domain.model.User;
import kz.projem.dto.request.TaskRequest;
import kz.projem.dto.response.TaskResponse;
import kz.projem.exception.ResourceNotFoundException;
import kz.projem.mapper.TaskMapper;
import kz.projem.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock TaskMapper taskMapper;
    @Mock AuditService auditService;

    @InjectMocks
    TaskService taskService;

    private User testUser;
    private Task testTask;
    private TaskRequest taskRequest;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        testTask = Task.builder()
                .id(1L)
                .title("Test task")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .user(testUser)
                .build();

        taskRequest = new TaskRequest();
        taskRequest.setTitle("Test task");
        taskRequest.setPriority(TaskPriority.MEDIUM);

        taskResponse = new TaskResponse();
        taskResponse.setId(1L);
        taskResponse.setTitle("Test task");
        taskResponse.setStatus(TaskStatus.TODO);
    }

    @Test
    void create_shouldReturnSavedTask() {
        when(taskMapper.toEntity(taskRequest)).thenReturn(testTask);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(taskMapper.toResponse(testTask)).thenReturn(taskResponse);

        TaskResponse result = taskService.create(taskRequest, testUser);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(taskRepository, times(1)).save(any(Task.class));
        verify(auditService).log(eq(1L), eq("test@example.com"), eq("CREATE_TASK"), any(), any(), any());
    }

    @Test
    void getById_whenNotExists_shouldThrowException() {
        when(taskRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getById(99L, testUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getById_whenExists_shouldReturnTask() {
        when(taskRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testTask));
        when(taskMapper.toResponse(testTask)).thenReturn(taskResponse);

        TaskResponse result = taskService.getById(1L, testUser);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test task");
    }

    @Test
    void complete_shouldSetStatusDoneAndCompletedAt() {
        when(taskRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(taskMapper.toResponse(any())).thenReturn(taskResponse);

        taskService.complete(1L, testUser);

        assertThat(testTask.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(testTask.getCompletedAt()).isNotNull();
    }

    @Test
    void delete_whenNotOwner_shouldThrowException() {
        when(taskRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(1L, testUser))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).delete(any(Task.class));
    }
}
