package com.jmens.advisor.modules.advisor.service;

import com.jmens.advisor.modules.advisor.domain.AdvisorReportSummary;
import com.jmens.advisor.modules.advisor.domain.AdvisorTaskResponse;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Executes one asynchronous advisor analysis task from the queue.
 *
 * <p>Each state transition is delegated to {@link AdvisorTaskService} so the
 * worker never keeps a long database transaction open while calling market data
 * APIs or LLM providers.</p>
 */
@Service
public class AdvisorTaskWorker {

  private final AdvisorTaskQueue queue;
  private final AdvisorTaskService taskService;
  private final AdvisorAnalysisService analysisService;
  private final AdvisorReportService reportService;

  public AdvisorTaskWorker(
      AdvisorTaskQueue queue,
      AdvisorTaskService taskService,
      AdvisorAnalysisService analysisService,
      AdvisorReportService reportService
  ) {
    this.queue = queue;
    this.taskService = taskService;
    this.analysisService = analysisService;
    this.reportService = reportService;
  }

  /**
   * Processes at most one queued task.
   *
   * @return {@code true} when a queue message was consumed, otherwise {@code false}
   */
  public boolean processNext() {
    Optional<AdvisorTaskMessage> optionalMessage = queue.poll();
    if (optionalMessage.isEmpty()) {
      return false;
    }
    AdvisorTaskMessage message = optionalMessage.get();
    try {
      AdvisorTaskResponse task = taskService.getTask(message.taskId());
      if (task.status() != AdvisorTaskStatus.PENDING) {
        return true;
      }
      taskService.markProcessing(task.taskId());
      ResearchReport report = analysisService.analyze(task.query(), task.analysisType());
      Long reportId = resolveLatestReportId(report);
      taskService.markCompleted(task.taskId(), reportId);
      return true;
    } catch (RuntimeException ex) {
      markFailedQuietly(message.taskId(), ex);
      return true;
    } finally {
      queue.ack(message.messageId());
    }
  }

  private Long resolveLatestReportId(ResearchReport report) {
    return reportService.findRecentReports(report.stockCode()).stream()
        .findFirst()
        .map(AdvisorReportSummary::id)
        .orElse(null);
  }

  private void markFailedQuietly(String taskId, RuntimeException ex) {
    try {
      taskService.markFailed(taskId, failureMessage(ex));
    } catch (RuntimeException ignored) {
      // The queue message is still acknowledged to avoid a hot loop on poison messages.
    }
  }

  private String failureMessage(RuntimeException ex) {
    String message = ex.getMessage();
    return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
  }
}
