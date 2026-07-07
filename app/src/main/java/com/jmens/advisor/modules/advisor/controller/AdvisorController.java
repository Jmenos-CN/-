package com.jmens.advisor.modules.advisor.controller;

import com.jmens.advisor.common.api.Result;
import com.jmens.advisor.modules.advisor.domain.AdvisorRequest;
import com.jmens.advisor.modules.advisor.domain.AdvisorReportSummary;
import com.jmens.advisor.modules.advisor.domain.AdvisorTaskResponse;
import com.jmens.advisor.modules.advisor.domain.FollowUpRequest;
import com.jmens.advisor.modules.advisor.domain.FollowUpResponse;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
import com.jmens.advisor.modules.advisor.service.AdvisorAnalysisService;
import com.jmens.advisor.modules.advisor.service.AdvisorFollowUpService;
import com.jmens.advisor.modules.advisor.service.AdvisorReportService;
import com.jmens.advisor.modules.advisor.service.AdvisorTaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/advisor")
public class AdvisorController {

  private final AdvisorAnalysisService advisorAnalysisService;
  private final AdvisorReportService advisorReportService;
  private final AdvisorTaskService advisorTaskService;
  private final AdvisorFollowUpService advisorFollowUpService;

  public AdvisorController(
      AdvisorAnalysisService advisorAnalysisService,
      AdvisorReportService advisorReportService,
      AdvisorTaskService advisorTaskService,
      AdvisorFollowUpService advisorFollowUpService
  ) {
    this.advisorAnalysisService = advisorAnalysisService;
    this.advisorReportService = advisorReportService;
    this.advisorTaskService = advisorTaskService;
    this.advisorFollowUpService = advisorFollowUpService;
  }

  @PostMapping("/analyze")
  public Result<ResearchReport> analyze(@Valid @RequestBody AdvisorRequest request) {
    return Result.ok(advisorAnalysisService.analyze(request.query(), request.analysisType()));
  }

  @GetMapping("/reports/{id}")
  public Result<ResearchReport> getReport(@PathVariable Long id) {
    return Result.ok(advisorReportService.getReport(id));
  }

  @GetMapping("/reports")
  public Result<List<AdvisorReportSummary>> findRecentReports(@RequestParam String stockCode) {
    return Result.ok(advisorReportService.findRecentReports(stockCode));
  }

  /**
   * Answers a follow-up question using the saved report as the grounding context.
   *
   * @param id source report ID
   * @param request user follow-up question
   * @return grounded answer with cited evidence and context source markers
   */
  @PostMapping("/reports/{id}/follow-up")
  public Result<FollowUpResponse> followUp(
      @PathVariable Long id,
      @Valid @RequestBody FollowUpRequest request
  ) {
    return Result.ok(advisorFollowUpService.answer(id, request.question()));
  }

  /**
   * Creates an asynchronous advisor analysis task.
   *
   * @param request user query and analysis type
   * @return task state that can be polled by task ID
   */
  @PostMapping("/tasks")
  public Result<AdvisorTaskResponse> createTask(@Valid @RequestBody AdvisorRequest request) {
    return Result.ok(advisorTaskService.createTask(request.query(), request.analysisType()));
  }

  /**
   * Reads the latest state of an asynchronous advisor analysis task.
   *
   * @param taskId task identifier returned by {@link #createTask(AdvisorRequest)}
   * @return task state, including report ID after completion
   */
  @GetMapping("/tasks/{taskId}")
  public Result<AdvisorTaskResponse> getTask(@PathVariable String taskId) {
    return Result.ok(advisorTaskService.getTask(taskId));
  }
}
