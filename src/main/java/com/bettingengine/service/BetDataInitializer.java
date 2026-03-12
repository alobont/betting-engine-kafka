package com.bettingengine.service;

import com.bettingengine.entity.BetEntity;
import com.bettingengine.repository.BetRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BetDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BetDataInitializer.class);

    private final BetRepository betRepository;

    public BetDataInitializer(BetRepository betRepository) {
        this.betRepository = betRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    public void seed() {
        betRepository.saveAll(defaultBets());
        log.info("Seeded Redis bets count={}", defaultBets().size());
    }

    private List<BetEntity> defaultBets() {
        return List.of(
                bet(1L, 101L, 1001L, 501L, 10L, "12.50"),
                bet(2L, 102L, 1001L, 502L, 11L, "20.00"),
                bet(3L, 103L, 2002L, 503L, 12L, "5.75"),
                bet(4L, 104L, 3003L, 504L, 21L, "18.25"),
                bet(5L, 105L, 3003L, 505L, 22L, "9.99"),
                bet(6L, 106L, 4004L, 506L, 31L, "42.10"),
                bet(7L, 107L, 5005L, 507L, 41L, "7.40"),
                bet(8L, 108L, 6006L, 508L, 51L, "15.00"),
                bet(9L, 109L, 6006L, 509L, 52L, "27.25")
        );
    }

    private BetEntity bet(Long betId, Long userId, Long eventId, Long marketId, Long winnerId, String amount) {
        return new BetEntity(
                betId,
                userId,
                eventId,
                marketId,
                winnerId,
                new BigDecimal(amount)
        );
    }
}
