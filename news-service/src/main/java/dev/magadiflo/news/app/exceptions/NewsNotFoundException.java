package dev.magadiflo.news.app.exceptions;

import dev.magadiflo.news.app.util.Constants;

public class NewsNotFoundException extends RuntimeException {
    public NewsNotFoundException(String date) {
        super(Constants.DATA_NOT_FOUND_MESSAGE.formatted(date));
    }
}
