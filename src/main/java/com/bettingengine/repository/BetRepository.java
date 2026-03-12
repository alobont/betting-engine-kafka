package com.bettingengine.repository;

import com.bettingengine.config.RedisStorageProperties;
import com.bettingengine.entity.BetEntity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BetRepository {

    private final StringRedisTemplate redisTemplate;
    private final RedisStorageProperties redisStorageProperties;

    public BetRepository(StringRedisTemplate redisTemplate, RedisStorageProperties redisStorageProperties) {
        this.redisTemplate = redisTemplate;
        this.redisStorageProperties = redisStorageProperties;
    }

    public void saveAll(Collection<BetEntity> bets) {
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        SetOperations<String, String> setOperations = redisTemplate.opsForSet();

        for (BetEntity bet : bets) {
            hashOperations.putAll(betKey(bet.getBetId()), serializedBet(bet));
            setOperations.add(eventIndexKey(bet.getEventId()), bet.getBetId().toString());
        }
    }

    public List<BetEntity> findByEventId(Long eventId) {
        Set<String> betIds = redisTemplate.opsForSet().members(eventIndexKey(eventId));
        if (betIds == null || betIds.isEmpty()) {
            return List.of();
        }

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        List<BetEntity> bets = new ArrayList<>();
        for (String betId : betIds) {
            Map<Object, Object> entries = hashOperations.entries(betKey(Long.parseLong(betId)));
            if (!entries.isEmpty()) {
                bets.add(deserializedBet(entries));
            }
        }

        bets.sort(Comparator.comparing(BetEntity::getBetId));
        return List.copyOf(bets);
    }

    private Map<String, String> serializedBet(BetEntity bet) {
        Map<String, String> fields = new HashMap<>();
        fields.put("betId", bet.getBetId().toString());
        fields.put("userId", bet.getUserId().toString());
        fields.put("eventId", bet.getEventId().toString());
        fields.put("eventMarketId", bet.getEventMarketId().toString());
        fields.put("eventWinnerId", bet.getEventWinnerId().toString());
        fields.put("betAmount", bet.getBetAmount().toPlainString());
        return fields;
    }

    private BetEntity deserializedBet(Map<Object, Object> entries) {
        return new BetEntity(
                Long.parseLong(stringValue(entries.get("betId"))),
                Long.parseLong(stringValue(entries.get("userId"))),
                Long.parseLong(stringValue(entries.get("eventId"))),
                Long.parseLong(stringValue(entries.get("eventMarketId"))),
                Long.parseLong(stringValue(entries.get("eventWinnerId"))),
                new BigDecimal(stringValue(entries.get("betAmount")))
        );
    }

    private String stringValue(Object value) {
        return Objects.toString(value, "");
    }

    private String betKey(Long betId) {
        return redisStorageProperties.getBetKeyPrefix() + ":" + betId;
    }

    private String eventIndexKey(Long eventId) {
        return redisStorageProperties.getEventIndexPrefix() + ":" + eventId;
    }
}
