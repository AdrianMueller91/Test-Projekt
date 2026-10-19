package de.testprojekt.application.service;

import java.time.Clock;
import java.util.List;

import de.testprojekt.application.port.in.CreateNoteUseCase;
import de.testprojekt.application.port.in.DeleteNoteUseCase;
import de.testprojekt.application.port.in.ListNotesUseCase;
import de.testprojekt.application.port.out.NoteRepository;
import de.testprojekt.domain.Note;
import de.testprojekt.domain.NoteId;
import de.testprojekt.domain.NoteNotFoundException;

/**
 * Anwendungsfälle rund um Notizen.
 *
 * Bewusst ohne Spring-Annotationen: Die Verdrahtung passiert in {@code config.ApplicationConfig}.
 * Jede Methode führt genau eine Datenbankoperation aus, daher ist keine Transaktionsklammer nötig.
 */
public class NoteService implements CreateNoteUseCase, ListNotesUseCase, DeleteNoteUseCase {

    private final NoteRepository repository;
    private final Clock clock;

    public NoteService(NoteRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Note create(String text) {
        Note note = Note.create(text, clock.instant());
        repository.save(note);
        return note;
    }

    @Override
    public List<Note> listNewestFirst() {
        return repository.findAllNewestFirst();
    }

    @Override
    public void delete(NoteId id) {
        if (!repository.deleteById(id)) {
            throw new NoteNotFoundException(id);
        }
    }
}
