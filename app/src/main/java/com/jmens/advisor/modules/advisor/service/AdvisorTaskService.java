package com.jmens.advisor.modules.advisor.service;

import com.jmens.advisor.modules.advisor.domain.AdvisorTaskResponse;
import com.jmens.advisor.modules.advisor.persistence.AdvisorTaskEntity;
import com.jmens.advisor.modules.advisor.persistence.AdvisorTaskRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates and tracks asynchronous advisor analysis tasks.
 */
@Service
public class AdvisorTaskService {

  private final AdvisorTaskRepository repository;
  private final AdvisorTaskQueue queue;

  public AdvisorTaskService(AdvisorTaskRepository repository, AdvisorTaskQueue queue) {
    this.repository = repository;
    this.queue = queue;
  }

  @Transactional
  public AdvisorTaskResponse createTask(String query, String analysisType) {
    String taskId = UUID.randomUUID().toString();
    LocalDateTime now = LocalDateTime.now();
    AdvisorTaskEntity entity = new AdvisorTaskEntity();
    entity.setTaskId(taskId);
    entity.setQuery(query);
    entity.setAnalysisType(normalizeAnalysisType(analysisType));
    entity.setStatus(AdvisorTaskStatus.PENDING);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    AdvisorTaskEntity saved = repository.save(entity);
    queue.publish(taskId);
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public AdvisorTaskResponse getTask(String taskId) {
    return repository.findById(taskId)
        .map(this::toResponse)
        .orElseThrow(() -> new IllegalArgumentException("Advisor task not found: " + taskId));
  }

  @Transactional
  public AdvisorTaskEntity markProcessing(String taskId) {
    AdvisorTaskEntity entity = getEntity(taskId);
    if (entity.getStatus() == AdvisorTaskStatus.PENDING) {
      entity.setStatus(AdvisorTaskStatus.PROCESSING);
      entity.setUpdatedAt(LocalDateTime.now());
    }
    return entity;
  }

  @Transactional
  public AdvisorTaskResponse markCompleted(String taskId, Long reportId) {
    AdvisorTaskEntity entity = getEntity(taskId);
    entity.setStatus(AdvisorTaskStatus.COMPLETED);
    entity.setReportId(reportId);
    entity.setErrorMessage(null);
    entity.setUpdatedAt(LocalDateTime.now());
    return toResponse(entity);
  }

  @Transactional
  public AdvisorTaskResponse markFailed(String taskId, String errorMessage) {
    AdvisorTaskEntity entity = getEntity(taskId);
    entity.setStatus(AdvisorTaskStatus.FAILED);
    entity.setErrorMessage(errorMessage);
    entity.setUpdatedAt(LocalDateTime.now());
    return toResponse(entity);
  }

  private AdvisorTaskEntity getEntity(String taskId) {
    return repository.findById(taskId)
        .orElseThrow(() -> new IllegalArgumentException("Advisor task not found: " + taskId));
  }

  private AdvisorTaskResponse toResponse(AdvisorTaskEntity entity) {
    return new AdvisorTaskResponse(
        entity.getTaskId(),
        entity.getQuery(),
        entity.getAnalysisType(),
        entity.getStatus(),
        entity.getReportId(),
        entity.getErrorMessage(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  private String normalizeAnalysisType(String analysisType) {
    return analysisType == null || analysisType.isBlank() ? "full" : analysisType;
  }
}
