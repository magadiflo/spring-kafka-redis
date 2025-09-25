package dev.magadiflo.worker.app.dao;

import dev.magadiflo.worker.app.external.news.dto.NewsResponse;
import reactor.core.publisher.Mono;

public interface NewsDao {
    Mono<NewsResponse> getNews(String date);

    Mono<Void> saveNews(String date, NewsResponse response);
}
