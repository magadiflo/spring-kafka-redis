package dev.magadiflo.news.app.dao.impl;

import dev.magadiflo.news.app.dao.NewsDao;
import dev.magadiflo.news.app.model.dto.response.NewsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Repository
public class NewsDaoImpl implements NewsDao {

    private final ReactiveRedisOperations<String, NewsResponse> reactiveRedisOperations;
    private static final String KEY_NEWS_REDIS = "news:%s";

    @Override
    public Mono<NewsResponse> getNews(String date) {
        return this.reactiveRedisOperations.opsForValue().get(KEY_NEWS_REDIS.formatted(date));
    }

}
