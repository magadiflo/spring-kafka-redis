package dev.magadiflo.news.app.service;

import reactor.core.publisher.Mono;

public interface NewsService {
    Mono<Object> getNews(String date);

    Mono<Void> publishToMessageBroker(String date);
}
