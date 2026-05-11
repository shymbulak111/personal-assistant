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
public class ChatResponse {
    private Long sessionId;
    private String sessionTitle;
    private Long messageId;
    private String reply;
    private List<String> actionsPerformed;
    private Integer tokensUsed;
    private LocalDateTime timestamp;
}
