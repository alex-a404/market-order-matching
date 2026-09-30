package org.ordermatching.api;

import org.ordermatching.handlerdomain.EngineRunner;
import org.ordermatching.handlerdomain.MatchingEngine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EngineProperties.class)
class EngineConfig {
    @Bean(initMethod = "start", destroyMethod = "stop")
    EngineRunner engineRunner(EngineProperties p) {
        return new EngineRunner(new MatchingEngine(p.symbols()), p.queueCapacity());
    }
}
