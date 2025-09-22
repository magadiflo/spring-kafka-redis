package dev.magadiflo.news.app.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record NewsResponse(Pagination pagination,
                           @JsonProperty("data")
                           List<NewsItem> items) {
}
