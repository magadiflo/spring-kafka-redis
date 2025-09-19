package dev.magadiflo.news.app.model.dto.response;

import dev.magadiflo.news.app.model.dto.response.enums.ErrorType;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(String code,
                            String message,
                            ErrorType errorType,
                            List<String> details,
                            LocalDateTime timestamp) {
}
