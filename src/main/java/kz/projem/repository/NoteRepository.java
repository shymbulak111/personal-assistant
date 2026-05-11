package kz.projem.repository;

import kz.projem.domain.model.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    Page<Note> findByUserIdAndArchived(Long userId, boolean archived, Pageable pageable);

    List<Note> findByUserIdAndPinnedTrueAndArchivedFalse(Long userId);

    Optional<Note> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT n FROM Note n WHERE n.user.id = :userId AND n.archived = false AND " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(n.content) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Note> search(@Param("userId") Long userId, @Param("q") String query, Pageable pageable);

    Page<Note> findByUserIdAndCategory(Long userId, String category, Pageable pageable);

    List<Note> findTop10ByUserIdAndArchivedFalseOrderByPinnedDescCreatedAtDesc(Long userId);

    List<Note> findAllByUserId(Long userId);
}
