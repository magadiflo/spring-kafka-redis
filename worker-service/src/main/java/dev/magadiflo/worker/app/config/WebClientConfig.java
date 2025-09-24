package dev.magadiflo.worker.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${external.services.news.base-url}")
    private String externalNewsUrl;

    @Bean
    public WebClient.Builder externalNewsClientBuilder() {
        return WebClient.builder()
                .baseUrl(this.externalNewsUrl);
    }
}
