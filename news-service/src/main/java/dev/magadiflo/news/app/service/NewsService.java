package dev.magadiflo.news.app.service;

import dev.magadiflo.news.app.model.dto.response.NewsResponse;
import reactor.core.publisher.Mono;

public interface NewsService {
    Mono<NewsResponse> getNews(String date);

    Mono<Void> publishToMessageBroker(String date);
}
