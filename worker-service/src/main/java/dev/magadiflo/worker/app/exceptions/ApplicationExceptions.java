package dev.magadiflo.worker.app.exceptions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationExceptions {
    public static <T> Mono<T> externalNewsNotFound(String date) {
        return Mono.error(() -> new ExternalNewsNotFoundException(date));
    }

    public static <T> Mono<T> externalInvalidNewsRequest(String message) {
        return Mono.error(() -> new ExternalInvalidNewsRequestException(message));
    }

    public static <T> Mono<T> externalServiceError(String message) {
        return Mono.error(() -> new ExternalServiceException(message));
    }
}
