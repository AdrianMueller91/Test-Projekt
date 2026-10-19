package de.testprojekt.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import de.testprojekt.application.port.out.NoteRepository;
import de.testprojekt.domain.Note;
import de.testprojekt.domain.NoteId;

/**
 * Ausgehender Adapter: speichert Notizen per JDBC in Postgres (lokal H2).
 * Tabelle siehe Flyway-Migration V1.
 */
@Repository
class JdbcNoteRepository implements NoteRepository {

    private final JdbcClient jdbc;

    JdbcNoteRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(Note note) {
        // Der Postgres-Treiber nimmt OffsetDateTime, aber kein Instant entgegen
        jdbc.sql("INSERT INTO note (id, text, created_at) VALUES (:id, :text, :createdAt)")
                .param("id", note.id().value())
                .param("text", note.text())
                .param("createdAt", OffsetDateTime.ofInstant(note.createdAt(), ZoneOffset.UTC))
                .update();
    }

    @Override
    public List<Note> findAllNewestFirst() {
        return jdbc.sql("SELECT id, text, created_at FROM note ORDER BY created_at DESC, id")
                .query(JdbcNoteRepository::map)
                .list();
    }

    @Override
    public boolean deleteById(NoteId id) {
        return jdbc.sql("DELETE FROM note WHERE id = :id")
                .param("id", id.value())
                .update() > 0;
    }

    private static Note map(ResultSet rs, int row) throws SQLException {
        return new Note(
                new NoteId(rs.getObject("id", UUID.class)),
                rs.getString("text"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant());
    }
}
