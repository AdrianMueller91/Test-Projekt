package de.testprojekt.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class NoteTest {

    private static final Instant NOW = Instant.parse("2026-10-19T10:00:00Z");

    @Test
    void trimsText() {
        assertThat(Note.create("  Einkaufen  ", NOW).text()).isEqualTo("Einkaufen");
    }

    @Test
    void rejectsBlankText() {
        assertThatThrownBy(() -> Note.create("   ", NOW)).isInstanceOf(InvalidNoteException.class);
        assertThatThrownBy(() -> Note.create(null, NOW)).isInstanceOf(InvalidNoteException.class);
    }

    @Test
    void rejectsTooLongText() {
        assertThat(Note.create("x".repeat(Note.MAX_LENGTH), NOW).text()).hasSize(Note.MAX_LENGTH);
        assertThatThrownBy(() -> Note.create("x".repeat(Note.MAX_LENGTH + 1), NOW))
                .isInstanceOf(InvalidNoteException.class);
    }
}
