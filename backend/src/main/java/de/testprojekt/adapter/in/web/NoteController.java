package de.testprojekt.adapter.in.web;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.testprojekt.application.port.in.CreateNoteUseCase;
import de.testprojekt.application.port.in.DeleteNoteUseCase;
import de.testprojekt.application.port.in.ListNotesUseCase;
import de.testprojekt.domain.Note;
import de.testprojekt.domain.NoteId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Eingehender Adapter: REST-Schnittstelle für die Angular-Oberfläche. Kennt nur die Use-Case-Ports. */
@RestController
@RequestMapping("/api/notes")
class NoteController {

    private final CreateNoteUseCase createNote;
    private final ListNotesUseCase listNotes;
    private final DeleteNoteUseCase deleteNote;

    NoteController(CreateNoteUseCase createNote, ListNotesUseCase listNotes, DeleteNoteUseCase deleteNote) {
        this.createNote = createNote;
        this.listNotes = listNotes;
        this.deleteNote = deleteNote;
    }

    @GetMapping
    List<NoteResponse> list() {
        return listNotes.listNewestFirst().stream().map(NoteResponse::from).toList();
    }

    @PostMapping
    ResponseEntity<NoteResponse> create(@Valid @RequestBody CreateNoteRequest request) {
        Note note = createNote.create(request.text());
        return ResponseEntity.created(URI.create("/api/notes/" + note.id())).body(NoteResponse.from(note));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteNote.delete(new NoteId(id));
        return ResponseEntity.noContent().build();
    }

    /** Eingabe-DTO. Die Längenprüfung hier liefert früh 400; die Fachregel selbst steht in {@link Note}. */
    record CreateNoteRequest(@NotBlank @Size(max = Note.MAX_LENGTH) String text) {
    }

    record NoteResponse(UUID id, String text, Instant createdAt) {
        static NoteResponse from(Note note) {
            return new NoteResponse(note.id().value(), note.text(), note.createdAt());
        }
    }
}
