package dev.magadiflo.news.app.dao.impl;

import dev.magadiflo.news.app.dao.NewsDao;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Repository
public class NewsDaoImpl implements NewsDao {

    private final ReactiveRedisOperations<String, Object> reactiveRedisOperations;

    @Override
    public Mono<Object> getNews(String date) {
        return this.reactiveRedisOperations.opsForValue().get(date);
    }

}
