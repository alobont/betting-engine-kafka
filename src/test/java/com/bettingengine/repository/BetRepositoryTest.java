package com.bettingengine.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bettingengine.config.RedisStorageProperties;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class BetRepositoryTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
    private final SetOperations<String, String> setOperations = mock(SetOperations.class);

    private final BetRepository betRepository = new BetRepository(redisTemplate, redisProperties());

    @Test
    void shouldFindBetsByEventIdFromRedis() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(setOperations.members("bets-by-event:1001")).thenReturn(Set.of("2", "1"));
        when(hashOperations.entries("bets:1")).thenReturn(Map.of(
                "betId", "1",
                "userId", "101",
                "eventId", "1001",
                "eventMarketId", "501",
                "eventWinnerId", "10",
                "betAmount", "12.50"
        ));
        when(hashOperations.entries("bets:2")).thenReturn(Map.of(
                "betId", "2",
                "userId", "102",
                "eventId", "1001",
                "eventMarketId", "502",
                "eventWinnerId", "11",
                "betAmount", "20.00"
        ));

        var bets = betRepository.findByEventId(1001L);

        assertThat(bets).extracting(com.bettingengine.entity.BetEntity::getBetId).containsExactly(1L, 2L);
        assertThat(bets).extracting(bet -> bet.getBetAmount().toPlainString()).containsExactly("12.50", "20.00");
    }

    @Test
    void shouldStoreBetsInRedisHashesAndEventIndexes() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        betRepository.saveAll(Set.of(new com.bettingengine.entity.BetEntity(
                9L,
                109L,
                9009L,
                509L,
                49L,
                new java.math.BigDecimal("17.35")
        )));

        verify(hashOperations).putAll("bets:9", Map.of(
                "betId", "9",
                "userId", "109",
                "eventId", "9009",
                "eventMarketId", "509",
                "eventWinnerId", "49",
                "betAmount", "17.35"
        ));
        verify(setOperations).add("bets-by-event:9009", "9");
    }

    private RedisStorageProperties redisProperties() {
        RedisStorageProperties properties = new RedisStorageProperties();
        properties.setBetKeyPrefix("bets");
        properties.setEventIndexPrefix("bets-by-event");
        properties.setSettlementKeyPrefix("settlement-claims");
        properties.setSettlementClaimTtl(java.time.Duration.ofMinutes(30));
        return properties;
    }
}
