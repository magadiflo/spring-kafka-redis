package dev.magadiflo.news.app.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NewsItem(String author,
                       String title,
                       String description,
                       String url,
                       String source,
                       String image,
                       String category,
                       String language,
                       String country,
                       @JsonProperty("published_at")
                       String publishedAt) {
}
