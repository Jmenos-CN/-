package com.jmens.advisor.common.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache.ttl")
public record CacheTtlProperties(
    Duration quote,
    Duration kline,
    Duration finance,
    Duration news,
    Duration report
) {

  public CacheTtlProperties {
    quote = quote == null ? Duration.ofSeconds(60) : quote;
    kline = kline == null ? Duration.ofHours(1) : kline;
    finance = finance == null ? Duration.ofDays(1) : finance;
    news = news == null ? Duration.ofMinutes(30) : news;
    report = report == null ? Duration.ofMinutes(5) : report;
  }
}
