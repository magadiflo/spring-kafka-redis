package dev.magadiflo.worker.app.external.news.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record NewsResponse(Pagination pagination,
                           @JsonProperty("data")
                           List<NewsItem> items) {
}
