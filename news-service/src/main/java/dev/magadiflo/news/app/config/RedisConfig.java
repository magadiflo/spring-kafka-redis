package dev.magadiflo.news.app.config;

import dev.magadiflo.news.app.model.dto.response.NewsResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Objects;

@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private Integer redisPort;

    @Value("${spring.data.redis.username}")
    private String redisUsername;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Bean("reactiveRedisConnectionFactory")
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        var config = new RedisStandaloneConfiguration();
        config.setHostName(Objects.requireNonNull(this.redisHost));
        config.setPort(Objects.requireNonNull(this.redisPort));
        config.setUsername(Objects.requireNonNull(this.redisUsername));
        config.setPassword(Objects.requireNonNull(this.redisPassword));
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public ReactiveRedisOperations<String, NewsResponse> reactiveRedisOperations(@Qualifier("reactiveRedisConnectionFactory") ReactiveRedisConnectionFactory factory) {
        var valueSerializer = new Jackson2JsonRedisSerializer<>(NewsResponse.class);
        var keySerializer = new StringRedisSerializer();

        var context = RedisSerializationContext.<String, NewsResponse>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

}
