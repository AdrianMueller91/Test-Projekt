package de.testprojekt.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Fachobjekt Notiz. Reines Java ohne Framework-Abhängigkeiten; die Regeln für gültigen Text stehen nur hier.
 */
public record Note(NoteId id, String text, Instant createdAt) {

    public static final int MAX_LENGTH = 500;

    public Note {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(createdAt, "createdAt");
        text = normalize(text);
    }

    /** Legt eine neue Notiz mit frischer ID an. */
    public static Note create(String text, Instant now) {
        return new Note(NoteId.random(), text, now);
    }

    private static String normalize(String text) {
        if (text == null || text.isBlank()) {
            throw new InvalidNoteException("Die Notiz darf nicht leer sein.");
        }
        String trimmed = text.strip();
        if (trimmed.length() > MAX_LENGTH) {
            throw new InvalidNoteException("Die Notiz darf höchstens " + MAX_LENGTH + " Zeichen haben.");
        }
        return trimmed;
    }
}
