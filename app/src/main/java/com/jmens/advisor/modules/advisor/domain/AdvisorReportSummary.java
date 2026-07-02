package com.jmens.advisor.modules.advisor.domain;

import java.time.LocalDateTime;

public record AdvisorReportSummary(
    Long id,
    String stockCode,
    String stockName,
    String quoteSummary,
    LocalDateTime createdAt
) {}
