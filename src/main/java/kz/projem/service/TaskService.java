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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final AuditService auditService;

    @Transactional
    public TaskResponse create(TaskRequest request, User user) {
        Task task = taskMapper.toEntity(request);
        task.setUser(user);

        if (task.getStatus() == null) task.setStatus(TaskStatus.TODO);
        if (task.getPriority() == null) task.setPriority(TaskPriority.MEDIUM);

        task = taskRepository.save(task);
        auditService.log(user.getId(), user.getEmail(), "CREATE_TASK", "TASK", task.getId(), task.getTitle());

        return taskMapper.toResponse(task);
    }

    public Page<TaskResponse> getAll(User user, Pageable pageable) {
        return taskRepository.findByUserId(user.getId(), pageable)
                .map(taskMapper::toResponse);
    }

    public Page<TaskResponse> getByStatus(User user, TaskStatus status, Pageable pageable) {
        return taskRepository.findByUserIdAndStatus(user.getId(), status, pageable)
                .map(taskMapper::toResponse);
    }

    public Page<TaskResponse> getByPriority(User user, TaskPriority priority, Pageable pageable) {
        return taskRepository.findByUserIdAndPriority(user.getId(), priority, pageable)
                .map(taskMapper::toResponse);
    }

    public Page<TaskResponse> search(User user, String query, Pageable pageable) {
        return taskRepository.searchTasks(user.getId(), query, pageable)
                .map(taskMapper::toResponse);
    }

    public TaskResponse getById(Long id, User user) {
        Task task = findTaskForUser(id, user.getId());
        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request, User user) {
        Task task = findTaskForUser(id, user.getId());
        taskMapper.updateFromRequest(request, task);

        // if marking as done, set completedAt
        if (request.getStatus() == TaskStatus.DONE && task.getCompletedAt() == null) {
            task.setCompletedAt(LocalDateTime.now());
        } else if (request.getStatus() != null && request.getStatus() != TaskStatus.DONE) {
            task.setCompletedAt(null);
        }

        task = taskRepository.save(task);
        auditService.log(user.getId(), user.getEmail(), "UPDATE_TASK", "TASK", task.getId(), task.getTitle());
        return taskMapper.toResponse(task);
    }

    @Transactional
    public void delete(Long id, User user) {
        Task task = findTaskForUser(id, user.getId());
        taskRepository.delete(task);
        auditService.log(user.getId(), user.getEmail(), "DELETE_TASK", "TASK", id, null);
    }

    @Transactional
    public TaskResponse complete(Long id, User user) {
        Task task = findTaskForUser(id, user.getId());
        task.setStatus(TaskStatus.DONE);
        task.setCompletedAt(LocalDateTime.now());
        task = taskRepository.save(task);
        auditService.log(user.getId(), user.getEmail(), "COMPLETE_TASK", "TASK", id, null);
        return taskMapper.toResponse(task);
    }

    private Task findTaskForUser(Long taskId, Long userId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
    }
}
