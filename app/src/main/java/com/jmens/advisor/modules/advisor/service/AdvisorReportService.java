package com.jmens.advisor.modules.advisor.service;

import com.jmens.advisor.modules.advisor.domain.AdvisorReportSummary;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
import com.jmens.advisor.modules.advisor.persistence.AdvisorReportEntity;
import com.jmens.advisor.modules.advisor.persistence.AdvisorReportRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists and reads generated advisor reports.
 */
@Service
public class AdvisorReportService {

  private final AdvisorReportRepository repository;
  private final AdvisorReportMapper mapper;

  public AdvisorReportService(AdvisorReportRepository repository, AdvisorReportMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Transactional
  public Long save(ResearchReport report) {
    AdvisorReportEntity saved = repository.save(mapper.toEntity(report));
    return saved.getId();
  }

  @Transactional(readOnly = true)
  public ResearchReport getReport(Long id) {
    return repository.findById(id)
        .map(mapper::toReport)
        .orElseThrow(() -> new IllegalArgumentException("Advisor report not found: " + id));
  }

  @Transactional(readOnly = true)
  public List<AdvisorReportSummary> findRecentReports(String stockCode) {
    return repository.findTop20ByStockCodeOrderByCreatedAtDesc(stockCode).stream()
        .map(entity -> new AdvisorReportSummary(
            entity.getId(),
            entity.getStockCode(),
            entity.getStockName(),
            entity.getQuoteSummary(),
            entity.getCreatedAt()
        ))
        .toList();
  }
}
