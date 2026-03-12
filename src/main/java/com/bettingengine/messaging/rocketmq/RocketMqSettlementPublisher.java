package com.bettingengine.messaging.rocketmq;

import com.bettingengine.config.TopicProperties;
import com.bettingengine.dto.messaging.SettlementMessage;
import com.bettingengine.entity.BetEntity;
import com.bettingengine.exception.MessagingPublishException;
import com.bettingengine.messaging.kafka.KafkaOutcomePublisher;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class RocketMqSettlementPublisher {

    private static final Logger log = LoggerFactory.getLogger(RocketMqSettlementPublisher.class);

    private final RocketMQTemplate rocketMQTemplate;
    private final TopicProperties topicProperties;

    public RocketMqSettlementPublisher(RocketMQTemplate rocketMQTemplate, TopicProperties topicProperties) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.topicProperties = topicProperties;
    }

    public void publish(BetEntity bet, String correlationId, String settlementKey) {
        SettlementMessage message = SettlementMessage.newBuilder()
                .setBetId(bet.getBetId())
                .setUserId(bet.getUserId())
                .setEventId(bet.getEventId())
                .setEventMarketId(bet.getEventMarketId())
                .setEventWinnerId(bet.getEventWinnerId())
                .setBetAmount(bet.getBetAmount().toPlainString())
                .build();

        Message<byte[]> rocketMessage = MessageBuilder.withPayload(message.toByteArray())
                .setHeader(RocketMQHeaders.KEYS, settlementKey)
                .setHeader(KafkaOutcomePublisher.CORRELATION_HEADER, correlationId)
                .build();

        try {
            rocketMQTemplate.syncSend(topicProperties.getBetSettlements(), rocketMessage);
            log.info(
                    "Published settlement message to RocketMQ topic={} betId={} correlationId={} settlementKey={}",
                    topicProperties.getBetSettlements(),
                    bet.getBetId(),
                    correlationId,
                    settlementKey
            );
        } catch (Exception exception) {
            throw new MessagingPublishException("Failed to publish settlement message to RocketMQ.", exception);
        }
    }
}
