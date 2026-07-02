package com.jmens.advisor.modules.advisor.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Persistent snapshot of one generated stock advisor report.
 */
@Entity
@Table(
    name = "stock_advisor_report",
    indexes = {
        @Index(name = "idx_stock_advisor_report_stock_created", columnList = "stock_code, created_at")
    }
)
public class AdvisorReportEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "stock_code", nullable = false, length = 16)
  private String stockCode;

  @Column(name = "stock_name", nullable = false, length = 64)
  private String stockName;

  @Column(name = "analysis_time", nullable = false)
  private LocalDateTime analysisTime;

  @Column(name = "quote_summary", nullable = false, columnDefinition = "text")
  private String quoteSummary;

  @Column(name = "fundamental_view", nullable = false, columnDefinition = "text")
  private String fundamentalView;

  @Column(name = "technical_view", nullable = false, columnDefinition = "text")
  private String technicalView;

  @Column(name = "valuation_view", nullable = false, columnDefinition = "text")
  private String valuationView;

  @Column(name = "news_view", nullable = false, columnDefinition = "text")
  private String newsView;

  @Column(name = "risk_view", nullable = false, columnDefinition = "text")
  private String riskView;

  @Column(nullable = false, columnDefinition = "text")
  private String conclusion;

  @Column(name = "evidences_json", nullable = false, columnDefinition = "text")
  private String evidencesJson;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getStockCode() {
    return stockCode;
  }

  public void setStockCode(String stockCode) {
    this.stockCode = stockCode;
  }

  public String getStockName() {
    return stockName;
  }

  public void setStockName(String stockName) {
    this.stockName = stockName;
  }

  public LocalDateTime getAnalysisTime() {
    return analysisTime;
  }

  public void setAnalysisTime(LocalDateTime analysisTime) {
    this.analysisTime = analysisTime;
  }

  public String getQuoteSummary() {
    return quoteSummary;
  }

  public void setQuoteSummary(String quoteSummary) {
    this.quoteSummary = quoteSummary;
  }

  public String getFundamentalView() {
    return fundamentalView;
  }

  public void setFundamentalView(String fundamentalView) {
    this.fundamentalView = fundamentalView;
  }

  public String getTechnicalView() {
    return technicalView;
  }

  public void setTechnicalView(String technicalView) {
    this.technicalView = technicalView;
  }

  public String getValuationView() {
    return valuationView;
  }

  public void setValuationView(String valuationView) {
    this.valuationView = valuationView;
  }

  public String getNewsView() {
    return newsView;
  }

  public void setNewsView(String newsView) {
    this.newsView = newsView;
  }

  public String getRiskView() {
    return riskView;
  }

  public void setRiskView(String riskView) {
    this.riskView = riskView;
  }

  public String getConclusion() {
    return conclusion;
  }

  public void setConclusion(String conclusion) {
    this.conclusion = conclusion;
  }

  public String getEvidencesJson() {
    return evidencesJson;
  }

  public void setEvidencesJson(String evidencesJson) {
    this.evidencesJson = evidencesJson;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
