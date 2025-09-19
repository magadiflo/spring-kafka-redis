package dev.magadiflo.news.app.service.impl;

import dev.magadiflo.news.app.dao.NewsDao;
import dev.magadiflo.news.app.exceptions.ApplicationExceptions;
import dev.magadiflo.news.app.service.NewsService;
import dev.magadiflo.news.app.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Service
public class NewsServiceImpl implements NewsService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final NewsDao newsDao;

    @Override
    public Mono<Object> getNews(String date) {
        return this.newsDao.getNews(date)
                .doOnNext(value -> log.info("Cache HIT - Obteniendo desde Redis para fecha: {}", value))
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Cache MISS - Publicando fecha {} en Kafka", date);
                    return this.publishToMessageBroker(date)
                            .then(ApplicationExceptions.newsNotFound(date));
                }));
    }

    @Override
    public Mono<Void> publishToMessageBroker(String date) {
        Message<String> message = MessageBuilder
                .withPayload(date)
                .setHeader(KafkaHeaders.TOPIC, Constants.TOPIC_NAME)
                .build();
        return Mono.fromFuture(() -> this.kafkaTemplate.send(message))
                .then();
    }
}
