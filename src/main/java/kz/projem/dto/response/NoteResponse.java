package kz.projem.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoteResponse {
    private Long id;
    private String title;
    private String content;
    private String tags;
    private String category;
    private boolean pinned;
    private boolean archived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
