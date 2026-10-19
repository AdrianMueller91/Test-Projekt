package de.testprojekt.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import de.testprojekt.domain.InvalidNoteException;
import de.testprojekt.domain.NoteNotFoundException;

/** Übersetzt fachliche Fehler in HTTP-Antworten (RFC 9457 Problem Details). */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(InvalidNoteException.class)
    ProblemDetail invalid(InvalidNoteException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NoteNotFoundException.class)
    ProblemDetail notFound(NoteNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
