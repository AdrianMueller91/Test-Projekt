package de.testprojekt.application.port.out;

import java.util.List;

import de.testprojekt.domain.Note;
import de.testprojekt.domain.NoteId;

/** Ausgehender Port zur Speicherung. Die Anwendung kennt nur dieses Interface, nicht die Datenbank. */
public interface NoteRepository {

    void save(Note note);

    List<Note> findAllNewestFirst();

    /** @return true, wenn eine Notiz gelöscht wurde */
    boolean deleteById(NoteId id);
}
