package com.api.FastShopping.config;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheManagerCustomizer() {
        return builder -> builder
                .withCacheConfiguration("products",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofSeconds(120))
                                .disableCachingNullValues()
                                .serializeValuesWith(
                                        RedisSerializationContext.SerializationPair.fromSerializer(
                                            RedisSerializer.json()
                                        )
                                )
                )
                .withCacheConfiguration("orderCount",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofSeconds(30))
                                .disableCachingNullValues()
                                .serializeValuesWith(
                                        RedisSerializationContext.SerializationPair.fromSerializer(
                                                RedisSerializer.json()
                                        )
                                )
                )
                .withCacheConfiguration("orderPages",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofSeconds(15))
                                .disableCachingNullValues()
                                .serializeValuesWith(
                                        RedisSerializationContext.SerializationPair.fromSerializer(
                                                RedisSerializer.json()
                                        )
                                )
                )
                .withCacheConfiguration("orders",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofSeconds(60))
                                .disableCachingNullValues()
                                .serializeValuesWith(
                                        RedisSerializationContext.SerializationPair.fromSerializer(
                                                RedisSerializer.json()
                                        )
                                )
                );
    }
}
