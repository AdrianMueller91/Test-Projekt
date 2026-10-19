package de.testprojekt.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.testprojekt.application.port.out.NoteRepository;
import de.testprojekt.domain.Note;
import de.testprojekt.domain.NoteId;
import de.testprojekt.domain.NoteNotFoundException;

/** Testet die Anwendungsfälle ohne Spring und ohne Datenbank, nur gegen einen Fake des ausgehenden Ports. */
class NoteServiceTest {

    private final InMemoryNoteRepository repository = new InMemoryNoteRepository();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-19T10:00:00Z"), ZoneOffset.UTC);
    private final NoteService service = new NoteService(repository, clock);

    @Test
    void createStoresNoteWithCurrentTime() {
        Note note = service.create("Hallo");

        assertThat(note.createdAt()).isEqualTo(clock.instant());
        assertThat(repository.findAllNewestFirst()).containsExactly(note);
    }

    @Test
    void deleteRemovesNote() {
        Note note = service.create("weg damit");

        service.delete(note.id());

        assertThat(service.listNewestFirst()).isEmpty();
    }

    @Test
    void deleteUnknownNoteFails() {
        assertThatThrownBy(() -> service.delete(NoteId.random())).isInstanceOf(NoteNotFoundException.class);
    }

    private static final class InMemoryNoteRepository implements NoteRepository {
        private final List<Note> notes = new ArrayList<>();

        @Override
        public void save(Note note) {
            notes.add(note);
        }

        @Override
        public List<Note> findAllNewestFirst() {
            return notes.stream().sorted(Comparator.comparing(Note::createdAt).reversed()).toList();
        }

        @Override
        public boolean deleteById(NoteId id) {
            return notes.removeIf(n -> n.id().equals(id));
        }
    }
}
