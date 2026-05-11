package kz.projem.dto.response;

import kz.projem.domain.enums.ReminderStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReminderResponse {
    private Long id;
    private String title;
    private String message;
    private LocalDateTime remindAt;
    private LocalDateTime sentAt;
    private ReminderStatus status;
    private int repeatMinutes;
    private LocalDateTime createdAt;
}
