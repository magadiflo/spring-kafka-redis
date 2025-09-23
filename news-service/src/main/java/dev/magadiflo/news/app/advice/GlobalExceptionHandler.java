package dev.magadiflo.news.app.advice;

import dev.magadiflo.news.app.exceptions.NewsNotFoundException;
import dev.magadiflo.news.app.model.dto.response.ErrorResponse;
import dev.magadiflo.news.app.model.dto.response.enums.ErrorType;
import dev.magadiflo.news.app.util.ErrorCatalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
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
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(HandlerMethodValidationException e) {
        e.getValueResults().forEach(result -> {
            String parameterName = result.getMethodParameter().getParameterName();
            List<String> messageList = result.getResolvableErrors().stream()
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .toList();
            log.warn("{}: {}", parameterName, messageList);
        });
        return this.buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCatalog.INVALID_PARAMETERS,
                ErrorType.FUNCTIONAL,
                e.getMessage());
    }

    @ExceptionHandler(NewsNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleNewsNotFoundException(NewsNotFoundException e) {
        log.error("{}", e.getMessage(), e);
        return this.buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ErrorCatalog.NEWS_NOT_FOUND,
                ErrorType.FUNCTIONAL,
                e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception e) {
        log.error("Excepción General: {}", e.getMessage(), e);
        return this.buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCatalog.INTERNAL_SERVER_ERROR,
                ErrorType.SYSTEM,
                e.getMessage());
    }

    private Mono<ResponseEntity<ErrorResponse>> buildErrorResponse(HttpStatus status, ErrorCatalog errorCatalog,
                                                                   ErrorType errorType, String details) {
        return Mono.just(ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        errorCatalog.getCode(),
                        errorCatalog.getMessage(),
                        errorType,
                        Collections.singletonList(details),
                        LocalDateTime.now())
                ));
    }

}
