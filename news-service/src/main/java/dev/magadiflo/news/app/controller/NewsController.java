package dev.magadiflo.news.app.controller;

import dev.magadiflo.news.app.model.dto.response.DataResponse;
import dev.magadiflo.news.app.service.NewsService;
import dev.magadiflo.news.app.util.Constants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/v1/news")
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public Mono<ResponseEntity<DataResponse<Object>>> getNews(@NotBlank(message = Constants.DATE_NOT_BLANK_MESSAGE)
                                                              @Pattern(regexp = Constants.DATE_FORMAT, message = Constants.DATE_PATTERN_MESSAGE)
                                                              @RequestParam(required = false) String date) {
        return this.newsService.getNews(date)
                .map(data -> ResponseEntity.ok(new DataResponse<>(Constants.DATA_FOUND_MESSAGE, Boolean.TRUE, data)));
    }

}
