# Bài Tập 1: Cấu hình Distributed Cache với Redis và Serializer JSON

Repository: [https://github.com/HoangCuongCat162006/Bai1__Ss17](https://github.com/HoangCuongCat162006/Bai1__Ss17)

## 1. Giới thiệu tổng quan
Dự án triển khai giải pháp **Distributed Cache** sử dụng **Redis** trong Spring Boot 3.x/4.x với các mục tiêu:
1. **Kết nối Redis an toàn, tối ưu**: Sử dụng `LettuceConnectionFactory` non-blocking tích hợp Connection Pooling (`commons-pool2`).
2. **Không hard-code**: Toàn bộ tham số kết nối (`host`, `port`, `timeout`, `pool`) được cấu hình tập trung trong `application.yml`.
3. **JSON Serialization**: Chuyển đổi từ Java Native Serializer (mặc định dạng nhị phân khó đọc) sang `GenericJackson2JsonRedisSerializer`. Dữ liệu cache lưu trong Redis ở định dạng JSON rõ ràng, có metadata `@class` để tương thích đa nền tảng và dễ dàng debug qua `redis-cli`.
4. **Sử dụng Annotation**: Kích hoạt `@EnableCaching` và minh họa `@Cacheable` trên tầng Service (`FoodService`).

---

## 2. Cấu trúc thư mục dự án
```text
Bai1__Ss17/
├── src/
│   ├── main/
│   │   ├── java/com/example/bai1__ss17/
│   │   │   ├── Bai1Ss17Application.java        # @SpringBootApplication, @EnableCaching
│   │   │   ├── config/
│   │   │   │   └── CacheConfig.java            # Cấu hình Bean RedisConnectionFactory, RedisCacheManager, RedisTemplate
│   │   │   ├── model/
│   │   │   │   └── Dish.java                   # Entity Dish (Serializable)
│   │   │   └── service/
│   │   │       └── FoodService.java            # Service demo với @Cacheable
│   │   └── resources/
│   │       └── application.yml                 # File cấu hình Redis parameters
│   └── test/
│       └── java/com/example/bai1__ss17/
│           └── Bai1Ss17ApplicationTests.java   # Unit & Integration Tests (Bean, JSON Serializer, Caching)
├── build.gradle                                # Khai báo dependencies
└── README.md
```

---

## 3. Chi tiết triển khai theo tiêu chí đánh giá

### 3.1. Khai báo Dependency (`build.gradle`)
Đã bổ sung đầy đủ starter Redis, connection pool và Jackson:
```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.apache.commons:commons-pool2'
    implementation 'com.fasterxml.jackson.core:jackson-databind'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

### 3.2. Cấu hình thông số (`application.yml`)
Không hard-code bất kỳ tham số nào trong code Java:
```yaml
spring:
  application:
    name: Bai1__Ss17
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 10
          max-idle: 8
          min-idle: 2
          max-wait: 1000ms
```

### 3.3. Lớp cấu hình `CacheConfig.java`
- Khởi tạo bean `RedisConnectionFactory` sử dụng `LettucePoolingClientConfiguration`.
- Khởi tạo bean `RedisCacheManager` sử dụng `GenericJackson2JsonRedisSerializer` với `ObjectMapper` có bật Default Typing (`@class`).
- Khởi tạo bean `RedisTemplate<String, Object>` với serializer tương ứng.

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.timeout:2000ms}")
    private Duration timeout;

    @Value("${spring.data.redis.lettuce.pool.max-active:10}")
    private int maxActive;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(host, port);

        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(maxActive);

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(timeout)
                .poolConfig(poolConfig)
                .build();

        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }
}
```

### 3.4. Service Demo `@Cacheable` (`FoodService.java`)
```java
@Service
public class FoodService {

    @Cacheable(value = "restaurantDishes", key = "#restaurantId")
    public List<Dish> getDishesByRestaurant(Long restaurantId) {
        System.out.println(">>> [DATABASE HIT] Đang truy vấn CSDL cho nhà hàng ID: " + restaurantId);
        List<Dish> dishes = new ArrayList<>();
        if (restaurantId == 1L) {
            dishes.add(new Dish(1L, "Phở Bò Tái Nạm", 55000.0, "Nước dùng thơm ngon"));
            dishes.add(new Dish(2L, "Bún Chả Hà Nội", 50000.0, "Thịt nướng than hoa"));
        }
        return dishes;
    }
}
```

---

## 4. Kiểm tra dữ liệu trong Redis CLI (JSON Serializer)
Khi gọi `foodService.getDishesByRestaurant(1L)`, kiểm tra qua `redis-cli`:
```bash
$ redis-cli
127.0.0.1:6379> KEYS *
1) "restaurantDishes::1"

127.0.0.1:6379> GET "restaurantDishes::1"
[
  "java.util.ArrayList",
  [
    {
      "@class": "com.example.bai1__ss17.model.Dish",
      "id": 1,
      "name": "Phở Bò Tái Nạm",
      "price": 55000.0,
      "description": "Nước dùng thơm ngon"
    },
    {
      "@class": "com.example.bai1__ss17.model.Dish",
      "id": 2,
      "name": "Bún Chả Hà Nội",
      "price": 50000.0,
      "description": "Thịt nướng than hoa"
    }
  ]
]
```
> **Nhận xét**: Dữ liệu lưu dưới dạng JSON rõ ràng, có trường `@class` hỗ trợ Jackson deserialize chính xác về kiểu `Dish` khi ứng dụng đọc từ Cache.

---

## 5. Hướng dẫn chạy và kiểm thử
Chạy toàn bộ unit & integration tests:
```bash
./gradlew test
```
Khởi động ứng dụng để chạy CommandLineRunner demo:
```bash
./gradlew bootRun
```
