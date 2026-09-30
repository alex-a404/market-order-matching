package org.ordermatching.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("engine")
record EngineProperties(List<String> symbols, int queueCapacity, Duration replyTimeout) {}
