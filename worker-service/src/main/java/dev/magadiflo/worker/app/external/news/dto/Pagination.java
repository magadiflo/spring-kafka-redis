package dev.magadiflo.worker.app.external.news.dto;

public record Pagination(int limit,
                         int offset,
                         int count,
                         int total) {
}
