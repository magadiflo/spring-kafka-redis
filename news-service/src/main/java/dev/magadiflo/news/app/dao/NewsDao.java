package dev.magadiflo.news.app.dao;

import dev.magadiflo.news.app.model.dto.response.NewsResponse;
import reactor.core.publisher.Mono;

public interface NewsDao {
    Mono<NewsResponse> getNews(String date);
}
