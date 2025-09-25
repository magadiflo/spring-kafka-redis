package dev.magadiflo.worker.app.client;

import dev.magadiflo.worker.app.exceptions.ApplicationExceptions;
import dev.magadiflo.worker.app.external.news.dto.NewsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class MediaStackServiceClient {

    private static final String ACCESS_KEY = "922887e602f7b3a4cdebdd6b3fd7a2c6";
    private static final String COUNTRIES = "pe";
    private static final int LIMIT = 5;

    private final WebClient client;

    public MediaStackServiceClient(WebClient.Builder builder) {
        this.client = builder.build();
    }

    public Mono<NewsResponse> getNews(String date) {
        log.info("Consultando noticias en MediaStack para la fecha: {}", date);
        return this.client.get()
                .uri("/v1/news", uriBuilder -> uriBuilder
                        .queryParam("access_key", ACCESS_KEY)
                        .queryParam("limit", LIMIT)
                        .queryParam("countries", COUNTRIES)
                        .queryParam("date", date)
                        .build())
                .retrieve()
                .bodyToMono(NewsResponse.class)
                .onErrorResume(WebClientResponseException.NotFound.class, exception -> {
                    log.warn("No se encontraron noticias en MediaStack para la fecha: {}. Código HTTP: {}, mensaje: {}",
                            date, exception.getStatusCode(), exception.getMessage());
                    return ApplicationExceptions.externalNewsNotFound(date);
                })
                .onErrorResume(WebClientResponseException.BadRequest.class, exception -> {
                    log.error("Solicitud inválida al API de MediaStack. Fecha: {}, código HTTP: {}, mensaje: {}",
                            date, exception.getStatusCode(), exception.getMessage());
                    return ApplicationExceptions.externalInvalidNewsRequest(exception.getMessage());
                })
                .onErrorResume(throwable -> {
                    log.error("Error inesperado consultando MediaStack. Fecha: {}, tipo: {}, mensaje: {}",
                            date, throwable.getClass().getSimpleName(), throwable.getMessage());
                    return ApplicationExceptions.externalServiceError(throwable.getMessage());
                });
    }
}
