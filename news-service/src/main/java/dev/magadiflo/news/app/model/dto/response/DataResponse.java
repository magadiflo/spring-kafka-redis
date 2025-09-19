package dev.magadiflo.news.app.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

public record DataResponse<T>(String message,
                              Boolean status,
                              @JsonInclude(JsonInclude.Include.NON_NULL)
                              T data) {
}
