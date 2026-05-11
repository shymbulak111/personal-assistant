package kz.projem.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
class RedisIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static org.testcontainers.containers.PostgreSQLContainer<?> postgres =
            new org.testcontainers.containers.PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("assistant_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void redis_setAndGet_works() {
        redisTemplate.opsForValue().set("test:hello", "world");
        assertThat(redisTemplate.opsForValue().get("test:hello")).isEqualTo("world");
    }

    @Test
    void redis_keyExpiry_works() throws InterruptedException {
        redisTemplate.opsForValue().set("test:expiry", "temp", Duration.ofSeconds(1));
        assertThat(redisTemplate.opsForValue().get("test:expiry")).isEqualTo("temp");
        Thread.sleep(1200);
        assertThat(redisTemplate.opsForValue().get("test:expiry")).isNull();
    }

    @Test
    void redis_deleteKey_works() {
        redisTemplate.opsForValue().set("test:delete", "value");
        redisTemplate.delete("test:delete");
        assertThat(redisTemplate.opsForValue().get("test:delete")).isNull();
    }
}
