package com.api.FastShopping.products.events;

import com.api.FastShopping.config.RedisStreamConfig;
import com.api.FastShopping.products.dtos.ProductDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WriteEventProducer {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void publishProductUpdate(String id, ProductDTO dto) {
        publish(RedisStreamConfig.PRODUCT_UPDATE_STREAM, Map.of(
                "id",    id,
                "name",  dto.name(),
                "price", String.valueOf(dto.price())
        ));
    }

    public void publishProductDelete(String id) {
        if (id == null || id.isBlank()) {
            log.warn("Attempted to publish delete with null id — skipping");
            return;
        }
        publish(RedisStreamConfig.PRODUCT_DELETE_STREAM, Map.of(
                "id", id
        ));
    }

    public void publishOrderCancel(String id) {
        if (id == null || id.isBlank()) {
            log.warn("Attempted to publish order cancel with null id — skipping");
            return;
        }
        publish(RedisStreamConfig.ORDER_CANCEL_STREAM, Map.of(
                "id", id
        ));
    }

    private void publish(String stream, Map<String, String> payload) {
        redisTemplate.opsForStream().add(
                StreamRecords.newRecord()
                        .in(stream)
                        .ofMap(payload)
        );
    }
}