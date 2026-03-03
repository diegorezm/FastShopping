package com.api.FastShopping.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisStreamConfig {
    public static final String PRODUCT_UPDATE_STREAM = "stream:products:update";
    public static final String PRODUCT_DELETE_STREAM = "stream:products:delete";
    public static final String ORDER_CANCEL_STREAM   = "stream:orders:cancel";

    public static final String PRODUCT_UPDATE_GROUP  = "group:products:update";
    public static final String PRODUCT_DELETE_GROUP  = "group:products:delete";
    public static final String ORDER_CANCEL_GROUP    = "group:orders:cancel";


    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }
}
