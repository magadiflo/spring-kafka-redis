package dev.magadiflo.worker.app.exceptions;

import dev.magadiflo.worker.app.util.Constants;

public class ExternalNewsNotFoundException extends RuntimeException {
    public ExternalNewsNotFoundException(String date) {
        super(Constants.DATA_NOT_FOUND_MESSAGE.formatted(date));
    }
}
