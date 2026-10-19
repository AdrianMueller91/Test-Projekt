package de.testprojekt.domain;

/** Angefragte Notiz existiert nicht. Der Web-Adapter macht daraus HTTP 404. */
public class NoteNotFoundException extends RuntimeException {

    public NoteNotFoundException(NoteId id) {
        super("Notiz " + id + " nicht gefunden.");
    }
}
