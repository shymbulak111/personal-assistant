package kz.projem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoteRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "Content cannot be empty")
    private String content;

    private String tags;

    @Size(max = 50)
    private String category;

    private boolean pinned;
}
