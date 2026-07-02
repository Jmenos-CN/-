package com.jmens.advisor.modules.advisor.persistence;

import com.jmens.advisor.modules.advisor.service.AdvisorTaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Tracks one asynchronous advisor analysis request.
 */
@Entity
@Table(name = "stock_advisor_task")
public class AdvisorTaskEntity {

  @Id
  @Column(name = "task_id", nullable = false, length = 36)
  private String taskId;

  @Column(nullable = false, columnDefinition = "text")
  private String query;

  @Column(name = "analysis_type", nullable = false, length = 32)
  private String analysisType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private AdvisorTaskStatus status;

  @Column(name = "report_id")
  private Long reportId;

  @Column(name = "error_message", columnDefinition = "text")
  private String errorMessage;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getAnalysisType() {
    return analysisType;
  }

  public void setAnalysisType(String analysisType) {
    this.analysisType = analysisType;
  }

  public AdvisorTaskStatus getStatus() {
    return status;
  }

  public void setStatus(AdvisorTaskStatus status) {
    this.status = status;
  }

  public Long getReportId() {
    return reportId;
  }

  public void setReportId(Long reportId) {
    this.reportId = reportId;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
