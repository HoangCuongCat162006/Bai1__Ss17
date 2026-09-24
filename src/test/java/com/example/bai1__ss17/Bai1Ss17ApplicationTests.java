package com.example.bai1__ss17;

import com.example.bai1__ss17.model.Dish;
import com.example.bai1__ss17.service.FoodService;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
class Bai1Ss17ApplicationTests {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private RedisCacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private FoodService foodService;

    @Test
    @DisplayName("Kiểm tra khởi tạo thành công các Bean Redis và Caching")
    void contextLoads() {
        Assertions.assertNotNull(redisConnectionFactory, "RedisConnectionFactory bean must not be null");
        Assertions.assertNotNull(cacheManager, "RedisCacheManager bean must not be null");
        Assertions.assertNotNull(redisTemplate, "RedisTemplate bean must not be null");
        Assertions.assertNotNull(foodService, "FoodService bean must not be null");

        Assertions.assertTrue(redisConnectionFactory instanceof LettuceConnectionFactory,
                "Connection factory must be LettuceConnectionFactory");

        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) redisConnectionFactory;
        Assertions.assertEquals("localhost", lettuceFactory.getHostName());
        Assertions.assertEquals(6379, lettuceFactory.getPort());
    }

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("Kiểm tra GenericJackson2JsonRedisSerializer tuần tự hoá JSON và giải tuần tự hoá chính xác")
    void testJsonSerialization() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        Dish originalDish = new Dish(1L, "Phở Bò Tái Nạm", 55000.0, "Nước dùng thơm ngon");

        byte[] serializedBytes = serializer.serialize(originalDish);
        Assertions.assertNotNull(serializedBytes);

        String jsonString = new String(serializedBytes, StandardCharsets.UTF_8);
        System.out.println("JSON Serialized Output: " + jsonString);

        Assertions.assertTrue(jsonString.contains("\"@class\""), "Serialized JSON must contain class metadata for generic deserialization");
        Assertions.assertTrue(jsonString.contains("Phở Bò Tái Nạm"), "Serialized JSON must contain dish name");
        Assertions.assertTrue(jsonString.contains("55000"), "Serialized JSON must contain price");

        Object deserialized = serializer.deserialize(serializedBytes);
        Assertions.assertNotNull(deserialized);
        Assertions.assertTrue(deserialized instanceof Dish);

        Dish deserializedDish = (Dish) deserialized;
        Assertions.assertEquals(originalDish.getId(), deserializedDish.getId());
        Assertions.assertEquals(originalDish.getName(), deserializedDish.getName());
        Assertions.assertEquals(originalDish.getPrice(), deserializedDish.getPrice());
    }

    @Test
    @DisplayName("Kiểm tra FoodService hoạt động và caching (nếu Redis khả dụng)")
    void testFoodServiceAndCaching() {
        Long restaurantId = 1L;

        boolean isRedisAvailable = false;
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String ping = connection.ping();
            isRedisAvailable = "PONG".equalsIgnoreCase(ping);
        } catch (Exception ignored) {
            System.out.println("Redis server hiện không chạy ở localhost:6379 (kiểm tra graceful test logic).");
        }

        if (isRedisAvailable) {
            // Lần 1: Cache Miss
            List<Dish> firstCall = foodService.getDishesByRestaurant(restaurantId);
            Assertions.assertNotNull(firstCall);
            Assertions.assertFalse(firstCall.isEmpty());

            // Lần 2: Cache Hit
            List<Dish> secondCall = foodService.getDishesByRestaurant(restaurantId);
            Assertions.assertNotNull(secondCall);
            Assertions.assertEquals(firstCall.size(), secondCall.size());
            Assertions.assertEquals(firstCall.get(0).getName(), secondCall.get(0).getName());
        } else {
            // Khi Redis offline, phương thức nghiệp vụ vẫn gọi được nếu không kích hoạt redis command
            System.out.println("Bỏ qua test live cache do Redis daemon chưa khởi động.");
        }
    }
}
