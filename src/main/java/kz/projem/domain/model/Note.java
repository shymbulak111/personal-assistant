package kz.projem.domain.model;

import jakarta.persistence.*;
import kz.projem.security.EncryptedStringConverter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // simple tagging
    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(length = 50)
    private String category;

    @Column(name = "is_pinned")
    @Builder.Default
    private boolean pinned = false;

    @Column(name = "is_archived")
    @Builder.Default
    private boolean archived = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
