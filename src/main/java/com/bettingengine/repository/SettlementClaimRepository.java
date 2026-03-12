package com.bettingengine.repository;

import com.bettingengine.config.RedisStorageProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SettlementClaimRepository {

    private static final String CLAIMED_VALUE = "claimed";

    private final StringRedisTemplate redisTemplate;
    private final RedisStorageProperties redisStorageProperties;

    public SettlementClaimRepository(StringRedisTemplate redisTemplate, RedisStorageProperties redisStorageProperties) {
        this.redisTemplate = redisTemplate;
        this.redisStorageProperties = redisStorageProperties;
    }

    public boolean claim(String settlementKey) {
        Boolean claimed = redisTemplate.opsForValue().setIfAbsent(
                redisKey(settlementKey),
                CLAIMED_VALUE,
                redisStorageProperties.getSettlementClaimTtl()
        );
        return Boolean.TRUE.equals(claimed);
    }

    public void release(String settlementKey) {
        redisTemplate.delete(redisKey(settlementKey));
    }

    private String redisKey(String settlementKey) {
        return redisStorageProperties.getSettlementKeyPrefix() + ":" + settlementKey;
    }
}
