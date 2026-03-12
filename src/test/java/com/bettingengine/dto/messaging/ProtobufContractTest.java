package com.bettingengine.dto.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Descriptors.FieldDescriptor;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ProtobufContractTest {

    @Test
    void shouldExposeExplicitEventOutcomeSchema() {
        Map<String, FieldDescriptor> fields = EventOutcomeMessage.getDescriptor().getFields().stream()
                .collect(Collectors.toMap(FieldDescriptor::getName, Function.identity()));

        assertThat(fields).containsKeys("event_id", "event_name", "event_winner_id");
        assertThat(fields.get("event_id").getType()).isEqualTo(FieldDescriptor.Type.INT64);
        assertThat(fields.get("event_name").getType()).isEqualTo(FieldDescriptor.Type.STRING);
        assertThat(fields.get("event_winner_id").getType()).isEqualTo(FieldDescriptor.Type.INT64);
    }

    @Test
    void shouldExposeExplicitSettlementSchemaAndPreserveAmountAsString() {
        Map<String, FieldDescriptor> fields = SettlementMessage.getDescriptor().getFields().stream()
                .collect(Collectors.toMap(FieldDescriptor::getName, Function.identity()));

        assertThat(fields).containsKeys("bet_id", "user_id", "event_id", "event_market_id", "event_winner_id", "bet_amount");
        assertThat(fields.get("bet_amount").getType()).isEqualTo(FieldDescriptor.Type.STRING);
    }
}
