package com.jmens.advisor.modules.advisor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jmens.advisor.modules.advisor.domain.DataEvidence;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
import com.jmens.advisor.modules.advisor.persistence.AdvisorReportEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Converts advisor report DTOs to stable database snapshots.
 */
@Component
public class AdvisorReportMapper {

  private final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  public AdvisorReportEntity toEntity(ResearchReport report) {
    AdvisorReportEntity entity = new AdvisorReportEntity();
    entity.setStockCode(report.stockCode());
    entity.setStockName(report.stockName());
    entity.setAnalysisTime(report.analysisTime());
    entity.setQuoteSummary(report.quoteSummary());
    entity.setFundamentalView(report.fundamentalView());
    entity.setTechnicalView(report.technicalView());
    entity.setValuationView(report.valuationView());
    entity.setNewsView(report.newsView());
    entity.setRiskView(report.riskView());
    entity.setConclusion(report.conclusion());
    entity.setEvidencesJson(writeEvidences(report.evidences()));
    entity.setCreatedAt(LocalDateTime.now());
    return entity;
  }

  public ResearchReport toReport(AdvisorReportEntity entity) {
    return new ResearchReport(
        entity.getStockCode(),
        entity.getStockName(),
        entity.getAnalysisTime(),
        entity.getQuoteSummary(),
        entity.getFundamentalView(),
        entity.getTechnicalView(),
        entity.getValuationView(),
        entity.getNewsView(),
        entity.getRiskView(),
        entity.getConclusion(),
        readEvidences(entity.getEvidencesJson())
    );
  }

  private String writeEvidences(List<DataEvidence> evidences) {
    try {
      return objectMapper.writeValueAsString(evidences);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize advisor report evidences", exception);
    }
  }

  private List<DataEvidence> readEvidences(String evidencesJson) {
    try {
      return objectMapper.readValue(evidencesJson, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize advisor report evidences", exception);
    }
  }
}
