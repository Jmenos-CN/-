package com.jmens.advisor.modules.advisor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jmens.advisor.modules.advisor.domain.DataEvidence;
import com.jmens.advisor.modules.advisor.domain.ReportInsight;
import com.jmens.advisor.modules.advisor.domain.ReportQuality;
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
    entity.setInsightsJson(writeInsights(report.insights()));
    entity.setQualityJson(writeQuality(report.quality()));
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
        readEvidences(entity.getEvidencesJson()),
        readInsights(entity.getInsightsJson()),
        readQuality(entity.getQualityJson())
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
    if (evidencesJson == null || evidencesJson.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(evidencesJson, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize advisor report evidences", exception);
    }
  }

  private String writeInsights(List<ReportInsight> insights) {
    try {
      return objectMapper.writeValueAsString(insights);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize advisor report insights", exception);
    }
  }

  private List<ReportInsight> readInsights(String insightsJson) {
    if (insightsJson == null || insightsJson.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(insightsJson, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize advisor report insights", exception);
    }
  }

  private String writeQuality(ReportQuality quality) {
    try {
      return objectMapper.writeValueAsString(quality == null ? ReportQuality.empty() : quality);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize advisor report quality", exception);
    }
  }

  private ReportQuality readQuality(String qualityJson) {
    if (qualityJson == null || qualityJson.isBlank()) {
      return ReportQuality.empty();
    }
    try {
      return objectMapper.readValue(qualityJson, ReportQuality.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize advisor report quality", exception);
    }
  }
}
