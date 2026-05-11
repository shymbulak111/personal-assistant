package kz.projem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {
    private long totalTasks;
    private long completedTasks;
    private long pendingTasks;
    private long overdueTasks;
    private long totalNotes;
    private long pinnedNotes;
    private long pendingReminders;
    private long totalChatSessions;
    private int aiRequestsUsed;
    private int aiRequestsLimit;
}
