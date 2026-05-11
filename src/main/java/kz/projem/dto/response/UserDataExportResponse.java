package kz.projem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDataExportResponse {
    private String email;
    private String fullName;
    private LocalDateTime accountCreated;
    private List<TaskResponse> tasks;
    private List<NoteResponse> notes;
    private List<ReminderResponse> reminders;
    private int totalChatSessions;
    private String exportedAt;
}
