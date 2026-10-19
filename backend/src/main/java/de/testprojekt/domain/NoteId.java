package de.testprojekt.domain;

import java.util.Objects;
import java.util.UUID;

/** Typsichere ID, damit keine beliebigen UUIDs als Notiz-ID durchgereicht werden. */
public record NoteId(UUID value) {

    public NoteId {
        Objects.requireNonNull(value, "value");
    }

    public static NoteId random() {
        return new NoteId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
