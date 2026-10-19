package de.testprojekt.application.port.in;

import de.testprojekt.domain.Note;

/** Eingehender Port: neue Notiz anlegen. */
public interface CreateNoteUseCase {

    Note create(String text);
}
