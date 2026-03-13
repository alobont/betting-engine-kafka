package com.bettingengine.repository;

import static org.apache.ignite.table.criteria.Criteria.columnValue;
import static org.apache.ignite.table.criteria.Criteria.equalTo;

import com.bettingengine.config.IgniteStorageProperties;
import com.bettingengine.entity.BetEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.lang.Cursor;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;
import org.springframework.stereotype.Repository;

@Repository
public class BetRepository {

    private final IgniteClient igniteClient;
    private final IgniteStorageProperties igniteStorageProperties;

    public BetRepository(IgniteClient igniteClient, IgniteStorageProperties igniteStorageProperties) {
        this.igniteClient = igniteClient;
        this.igniteStorageProperties = igniteStorageProperties;
    }

    public void saveAll(Collection<BetEntity> bets) {
        if (bets.isEmpty()) {
            return;
        }

        List<Tuple> rows = bets.stream()
                .map(this::betTuple)
                .toList();
        betRecordView().upsertAll(null, rows);
    }

    public List<BetEntity> findByEventId(Long eventId) {
        try (Cursor<Tuple> cursor = betRecordView().query(null, columnValue("EVENT_ID", equalTo(eventId)))) {
            List<BetEntity> bets = new ArrayList<>();
            while (cursor.hasNext()) {
                bets.add(mapBet(cursor.next()));
            }
            bets.sort(Comparator.comparing(BetEntity::getBetId));
            return List.copyOf(bets);
        }
    }

    private RecordView<Tuple> betRecordView() {
        return requireTable(tableIdentifier()).recordView();
    }

    private Tuple betTuple(BetEntity bet) {
        return Tuple.create()
                .set("EVENT_ID", bet.getEventId())
                .set("BET_ID", bet.getBetId())
                .set("USER_ID", bet.getUserId())
                .set("EVENT_MARKET_ID", bet.getEventMarketId())
                .set("EVENT_WINNER_ID", bet.getEventWinnerId())
                .set("BET_AMOUNT", bet.getBetAmount());
    }

    private BetEntity mapBet(Tuple row) {
        return new BetEntity(
                row.longValue("BET_ID"),
                row.longValue("USER_ID"),
                row.longValue("EVENT_ID"),
                row.longValue("EVENT_MARKET_ID"),
                row.longValue("EVENT_WINNER_ID"),
                row.decimalValue("BET_AMOUNT")
        );
    }

    private Table requireTable(String tableName) {
        Table table = igniteClient.tables().table(tableName);
        if (table == null) {
            throw new IllegalStateException("Ignite table is not available: " + tableName);
        }
        return table;
    }

    private String tableIdentifier() {
        return igniteStorageProperties.getBetTableName();
    }
}
