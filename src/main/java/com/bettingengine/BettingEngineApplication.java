package com.bettingengine;

import com.bettingengine.config.IgniteStorageProperties;
import com.bettingengine.config.TopicProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({TopicProperties.class, IgniteStorageProperties.class})
public class BettingEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(BettingEngineApplication.class, args);
    }
}
