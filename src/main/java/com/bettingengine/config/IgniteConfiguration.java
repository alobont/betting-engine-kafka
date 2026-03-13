package com.bettingengine.config;

import java.time.Duration;
import java.util.List;
import org.apache.ignite.client.IgniteClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IgniteConfiguration {

    @Bean(destroyMethod = "close")
    public IgniteClient igniteClient(IgniteStorageProperties igniteStorageProperties) {
        List<String> addresses = igniteStorageProperties.getAddresses();
        if (addresses == null || addresses.isEmpty()) {
            throw new IllegalStateException("At least one Ignite client address must be configured.");
        }

        return IgniteClient.builder()
                .addresses(addresses.toArray(String[]::new))
                .connectTimeout(Duration.ofSeconds(10).toMillis())
                .backgroundReconnectInterval(Duration.ofSeconds(3).toMillis())
                .operationTimeout(Duration.ofSeconds(60).toMillis())
                .build();
    }
}
