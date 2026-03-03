package com.api.FastShopping.products.events;

import com.api.FastShopping.config.RedisStreamConfig;
import com.api.FastShopping.products.dtos.ProductDTO;
import com.api.FastShopping.products.services.OrderService;
import com.api.FastShopping.products.services.ProductService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WriteEventConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final ProductService productService;
    private final OrderService orderService;

    @PostConstruct
    public void initStreams() {
        createStreamAndGroup(
                RedisStreamConfig.PRODUCT_UPDATE_STREAM,
                RedisStreamConfig.PRODUCT_UPDATE_GROUP
        );
        createStreamAndGroup(
                RedisStreamConfig.PRODUCT_DELETE_STREAM,
                RedisStreamConfig.PRODUCT_DELETE_GROUP
        );
        createStreamAndGroup(
                RedisStreamConfig.ORDER_CANCEL_STREAM,
                RedisStreamConfig.ORDER_CANCEL_GROUP
        );
    }

    private void createStreamAndGroup(String stream, String group) {
        try {
            RecordId bootstrapId = redisTemplate.opsForStream().add(
                    StreamRecords.newRecord()
                            .in(stream)
                            .ofMap(Map.of("bootstrap", "true"))
            );
            redisTemplate.opsForStream()
                    .createGroup(stream, ReadOffset.from(bootstrapId.getValue()), group);
            log.info("Created stream {} with group {}", stream, group);
        } catch (Exception e) {
            log.debug("Stream/group already exists: {} / {}", stream, group);
        }
    }

    // --- Product Update ---
    @Scheduled(fixedDelay = 100)
    public void processProductUpdates() {
        poll(
                RedisStreamConfig.PRODUCT_UPDATE_STREAM,
                RedisStreamConfig.PRODUCT_UPDATE_GROUP,
                "worker-1",
                message -> {
                    String id = message.getValue().get("id");
                    String name = message.getValue().get("name");
                    double price = Double.parseDouble(message.getValue().get("price"));
                    if (id != null && !id.isBlank()) {
                        productService.update(id, new ProductDTO(name, BigDecimal.valueOf(price)));
                    }
                }
        );
    }

    // --- Product Delete ---
    @Scheduled(fixedDelay = 100)
    public void processProductDeletes() {
        poll(
                RedisStreamConfig.PRODUCT_DELETE_STREAM,
                RedisStreamConfig.PRODUCT_DELETE_GROUP,
                "worker-1",
                message -> {
                    String id = message.getValue().get("id");
                    if (id != null && !id.isBlank()) {
                        productService.delete(id);
                    }
                }
        );
    }

    // --- Order Cancel ---
    @Scheduled(fixedDelay = 100)
    public void processOrderCancels() {
        poll(
                RedisStreamConfig.ORDER_CANCEL_STREAM,
                RedisStreamConfig.ORDER_CANCEL_GROUP,
                "worker-1",
                message -> {
                    String id = message.getValue().get("id");
                    if (id != null && !id.isBlank()) {
                        orderService.cancel(UUID.fromString(id));
                    }
                }
        );
    }

    // --- Shared poll helper ---
    private void poll(
            String stream,
            String group,
            String consumer,
            java.util.function.Consumer<MapRecord<String, String, String>> handler) {
        try {
            // Use typed stream ops instead of generic opsForStream()
            StreamOperations<String, String, String> streamOps =
                    redisTemplate.opsForStream();

            List<MapRecord<String, String, String>> messages = streamOps.read(
                    Consumer.from(group, consumer),
                    StreamReadOptions.empty().count(10),
                    StreamOffset.create(stream, ReadOffset.lastConsumed())
            );

            if (messages == null || messages.isEmpty()) return;

            for (MapRecord<String, String, String> message : messages) {
                try {
                    handler.accept(message);
                    streamOps.acknowledge(stream, group, message.getId());
                } catch (Exception e) {
                    log.error("Failed to process message {} from {}: {}",
                            message.getId(), stream, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Poll failed for stream {}: {}", stream, e.getMessage());
        }
    }
}