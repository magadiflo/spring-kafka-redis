package dev.magadiflo.worker.app.exceptions;

public class ExternalInvalidNewsRequestException extends RuntimeException {
    public ExternalInvalidNewsRequestException(String message) {
        super(message);
    }
}
