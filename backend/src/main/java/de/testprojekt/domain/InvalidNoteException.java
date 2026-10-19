package de.testprojekt.domain;

/** Verletzte Fachregel einer Notiz. Der Web-Adapter macht daraus HTTP 400. */
public class InvalidNoteException extends RuntimeException {

    public InvalidNoteException(String message) {
        super(message);
    }
}
