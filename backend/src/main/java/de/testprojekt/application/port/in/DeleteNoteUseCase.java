package de.testprojekt.application.port.in;

import de.testprojekt.domain.NoteId;
import de.testprojekt.domain.NoteNotFoundException;

/** Eingehender Port: Notiz löschen. */
public interface DeleteNoteUseCase {

    /** @throws NoteNotFoundException wenn es die Notiz nicht gibt */
    void delete(NoteId id);
}
