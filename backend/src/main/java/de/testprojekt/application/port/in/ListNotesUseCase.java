package de.testprojekt.application.port.in;

import java.util.List;

import de.testprojekt.domain.Note;

/** Eingehender Port: alle Notizen, neueste zuerst. */
public interface ListNotesUseCase {

    List<Note> listNewestFirst();
}
