package com.jmens.advisor.modules.stock.domain;

import java.time.LocalDateTime;

public record StockNewsItem(
    String title,
    String url,
    LocalDateTime publishedAt,
    String source,
    String summary
) {

  public StockNewsItem(String title, String url, LocalDateTime publishedAt, String source) {
    this(title, url, publishedAt, source, "");
  }
}
