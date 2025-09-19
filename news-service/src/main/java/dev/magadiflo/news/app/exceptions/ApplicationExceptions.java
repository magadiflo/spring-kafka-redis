package dev.magadiflo.news.app.exceptions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationExceptions {
    public static <T> Mono<T> newsNotFound(String date) {
        return Mono.error(() -> new NewsNotFoundException(date));
    }
}
