package dev.magadiflo.news.app.advice;

import dev.magadiflo.news.app.model.dto.response.ErrorResponse;
import dev.magadiflo.news.app.model.dto.response.enums.ErrorType;
import dev.magadiflo.news.app.util.ErrorCatalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HandlerMethodValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleException(HandlerMethodValidationException e) {
        e.getValueResults().forEach(result -> {
            String parameterName = result.getMethodParameter().getParameterName();
            List<String> messageList = result.getResolvableErrors().stream()
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .toList();
            log.warn("{}: {}", parameterName, messageList);
        });

        return Mono.fromSupplier(() -> ResponseEntity
                .badRequest()
                .body(new ErrorResponse(
                        ErrorCatalog.INVALID_PARAMETERS.getCode(),
                        ErrorCatalog.INVALID_PARAMETERS.getMessage(),
                        ErrorType.FUNCTIONAL,
                        Collections.singletonList(e.getMessage()),
                        LocalDateTime.now())
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleException(Exception e) {
        log.error("Excepción General: {}", e.getMessage(), e);
        return Mono.fromSupplier(() -> ResponseEntity
                .internalServerError()
                .body(new ErrorResponse(
                        ErrorCatalog.INTERVAL_SERVER_ERROR.getCode(),
                        ErrorCatalog.INTERVAL_SERVER_ERROR.getMessage(),
                        ErrorType.SYSTEM,
                        Collections.singletonList(e.getMessage()),
                        LocalDateTime.now())
                ));
    }

}
