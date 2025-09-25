package dev.magadiflo.worker.app.listener;

import dev.magadiflo.worker.app.client.MediaStackServiceClient;
import dev.magadiflo.worker.app.dao.NewsDao;
import dev.magadiflo.worker.app.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Component
public class NewsKafkaConsumer {

    private final MediaStackServiceClient externalClient;
    private final NewsDao newsDao;

    @KafkaListener(topics = Constants.TOPIC_NEWS, groupId = Constants.GROUP_ID_NEWS_TOPIC)
    public void consumeDateToSearchNews(String date) {

        log.info("Recibiendo fecha desde Kafka: {}", date);

        this.newsDao.getNews(date)
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("No existe noticia en Redis para fecha: {}, consultando API externa", date);
                    return this.externalClient.getNews(date)
                            .flatMap(newsResponse -> this.newsDao.saveNews(date, newsResponse)
                                    .thenReturn(newsResponse));
                }))
                .doOnNext(newsResponse -> log.info("Procesamiento completado exitosamente para fecha: {}", date))
                .doOnError(throwable -> log.error("Error procesando fecha {}: {}", date, throwable.getMessage()))
                .subscribe();
    }

}
