package com.jmens.advisor.modules.stock.domain;

import java.time.LocalDateTime;

public record StockNewsItem(
    String title,
    String url,
    LocalDateTime publishedAt,
    String source
) {}
