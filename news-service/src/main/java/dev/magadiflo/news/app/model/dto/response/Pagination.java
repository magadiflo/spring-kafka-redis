package dev.magadiflo.news.app.model.dto.response;

public record Pagination(int limit,
                         int offset,
                         int count,
                         int total) {
}
