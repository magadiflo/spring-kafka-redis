package dev.magadiflo.worker.app.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
    @Value("${redisson.host}")
    private String redisHost;

    @Value("${redisson.port}")
    private Integer redisPort;

    @Value("${redisson.username}")
    private String redisUsername;

    @Value("${redisson.password}")
    private String redisPassword;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://%s:%s".formatted(this.redisHost, this.redisPort))
                .setUsername(this.redisUsername)
                .setPassword(this.redisPassword);
        return Redisson.create(config);
    }

    @Bean
    public RedissonReactiveClient redissonReactiveClient(RedissonClient redissonClient) {
        return redissonClient.reactive();
    }
}
