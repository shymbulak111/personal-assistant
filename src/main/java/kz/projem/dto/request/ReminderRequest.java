package kz.projem.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReminderRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String message;

    @NotNull(message = "Remind time is required")
    @Future(message = "Reminder must be set in the future")
    private LocalDateTime remindAt;

    private int repeatMinutes;
}
