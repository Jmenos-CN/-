package com.jmens.advisor.modules.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jmens.advisor.modules.advisor.domain.AdvisorReportSummary;
import com.jmens.advisor.modules.advisor.domain.AdvisorTaskResponse;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class AdvisorTaskWorkerTest {

  @Test
  void processesOnePendingTaskAndMarksItCompleted() {
    AdvisorTaskQueue queue = mock(AdvisorTaskQueue.class);
    AdvisorTaskService taskService = mock(AdvisorTaskService.class);
    AdvisorAnalysisService analysisService = mock(AdvisorAnalysisService.class);
    AdvisorReportService reportService = mock(AdvisorReportService.class);
    when(queue.poll()).thenReturn(Optional.of(new AdvisorTaskMessage("message-1", "task-1")));
    when(taskService.getTask("task-1")).thenReturn(task("task-1", AdvisorTaskStatus.PENDING));
    when(analysisService.analyze("帮我分析600519", "full")).thenReturn(report());
    when(reportService.findRecentReports("600519")).thenReturn(List.of(reportSummary(9L)));

    AdvisorTaskWorker worker = new AdvisorTaskWorker(
        queue,
        taskService,
        analysisService,
        reportService
    );

    assertThat(worker.processNext()).isTrue();
    InOrder inOrder = inOrder(taskService, analysisService, reportService, queue);
    inOrder.verify(taskService).markProcessing("task-1");
    inOrder.verify(analysisService).analyze("帮我分析600519", "full");
    inOrder.verify(reportService).findRecentReports("600519");
    inOrder.verify(taskService).markCompleted("task-1", 9L);
    inOrder.verify(queue).ack("message-1");
  }

  @Test
  void marksTaskFailedAndAcknowledgesMessageWhenAnalysisFails() {
    AdvisorTaskQueue queue = mock(AdvisorTaskQueue.class);
    AdvisorTaskService taskService = mock(AdvisorTaskService.class);
    AdvisorAnalysisService analysisService = mock(AdvisorAnalysisService.class);
    AdvisorReportService reportService = mock(AdvisorReportService.class);
    when(queue.poll()).thenReturn(Optional.of(new AdvisorTaskMessage("message-1", "task-1")));
    when(taskService.getTask("task-1")).thenReturn(task("task-1", AdvisorTaskStatus.PENDING));
    when(analysisService.analyze("帮我分析600519", "full"))
        .thenThrow(new IllegalStateException("provider unavailable"));

    AdvisorTaskWorker worker = new AdvisorTaskWorker(
        queue,
        taskService,
        analysisService,
        reportService
    );

    assertThat(worker.processNext()).isTrue();
    verify(taskService).markFailed("task-1", "provider unavailable");
    verify(queue).ack("message-1");
  }

  @Test
  void returnsFalseWhenQueueIsEmpty() {
    AdvisorTaskQueue queue = mock(AdvisorTaskQueue.class);
    AdvisorTaskService taskService = mock(AdvisorTaskService.class);
    AdvisorAnalysisService analysisService = mock(AdvisorAnalysisService.class);
    AdvisorReportService reportService = mock(AdvisorReportService.class);
    when(queue.poll()).thenReturn(Optional.empty());

    AdvisorTaskWorker worker = new AdvisorTaskWorker(
        queue,
        taskService,
        analysisService,
        reportService
    );

    assertThat(worker.processNext()).isFalse();
    verifyNoInteractions(taskService, analysisService, reportService);
  }

  private AdvisorTaskResponse task(String taskId, AdvisorTaskStatus status) {
    return new AdvisorTaskResponse(
        taskId,
        "帮我分析600519",
        "full",
        status,
        null,
        null,
        LocalDateTime.now(),
        LocalDateTime.now()
    );
  }

  private ResearchReport report() {
    return new ResearchReport(
        "600519",
        "贵州茅台",
        LocalDateTime.now(),
        "行情摘要",
        "基本面",
        "技术面",
        "估值",
        "新闻",
        "风险",
        "结论",
        List.of()
    );
  }

  private AdvisorReportSummary reportSummary(Long id) {
    return new AdvisorReportSummary(id, "600519", "贵州茅台", "行情摘要", LocalDateTime.now());
  }
}
