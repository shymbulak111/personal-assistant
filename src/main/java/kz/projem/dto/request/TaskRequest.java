package kz.projem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kz.projem.domain.enums.TaskPriority;
import kz.projem.domain.enums.TaskStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskRequest {

    @NotBlank(message = "Title cannot be empty")
    @Size(max = 255)
    private String title;

    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    private LocalDate dueDate;

    @Size(max = 50)
    private String category;

    private String tags;
}
