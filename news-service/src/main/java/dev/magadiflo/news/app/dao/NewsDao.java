package dev.magadiflo.news.app.dao;

import reactor.core.publisher.Mono;

public interface NewsDao {
    Mono<Object> getNews(String date);
}
