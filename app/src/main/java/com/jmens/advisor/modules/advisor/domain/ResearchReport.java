package com.jmens.advisor.modules.advisor.domain;

import java.time.LocalDateTime;
import java.util.List;

public record ResearchReport(
    String stockCode,
    String stockName,
    LocalDateTime analysisTime,
    String quoteSummary,
    String fundamentalView,
    String technicalView,
    String valuationView,
    String newsView,
    String riskView,
    String conclusion,
    List<DataEvidence> evidences
) {}
