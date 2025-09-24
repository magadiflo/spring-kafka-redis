package dev.magadiflo.worker.app.dao.impl;

import dev.magadiflo.worker.app.dao.NewsDao;
import dev.magadiflo.worker.app.external.news.dto.NewsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucketReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Repository
public class NewsDaoImpl implements NewsDao {

    private final RedissonReactiveClient client;
    private static final String KEY_NEWS_REDIS = "news:%s";
    private static final TypedJsonJacksonCodec NEWS_CODEC = new TypedJsonJacksonCodec(NewsResponse.class);

    @Override
    public Mono<Void> saveNews(String date, NewsResponse response) {
        String key = KEY_NEWS_REDIS.formatted(date);
        log.info("Guardando noticia en Redis con clave: {}", key);

        RBucketReactive<NewsResponse> bucket = this.client.getBucket(key, NEWS_CODEC);
        return bucket.set(response, Duration.ofHours(1L));
    }
}
