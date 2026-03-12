package com.bettingengine.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bettingengine.config.RedisStorageProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class SettlementClaimRepositoryTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

    private final SettlementClaimRepository settlementClaimRepository =
            new SettlementClaimRepository(redisTemplate, redisProperties());

    @Test
    void shouldClaimSettlementKeyWithConfiguredTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("settlement-claims:1:1001", "claimed", Duration.ofMinutes(30))).thenReturn(true);

        boolean claimed = settlementClaimRepository.claim("1:1001");

        assertThat(claimed).isTrue();
    }

    @Test
    void shouldReleaseSettlementKeyFromRedis() {
        settlementClaimRepository.release("1:1001");

        verify(redisTemplate).delete("settlement-claims:1:1001");
    }

    private RedisStorageProperties redisProperties() {
        RedisStorageProperties properties = new RedisStorageProperties();
        properties.setBetKeyPrefix("bets");
        properties.setEventIndexPrefix("bets-by-event");
        properties.setSettlementKeyPrefix("settlement-claims");
        properties.setSettlementClaimTtl(Duration.ofMinutes(30));
        return properties;
    }
}
