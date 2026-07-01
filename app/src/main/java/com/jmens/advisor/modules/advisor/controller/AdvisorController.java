package com.jmens.advisor.modules.advisor.controller;

import com.jmens.advisor.common.api.Result;
import com.jmens.advisor.modules.advisor.domain.AdvisorRequest;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
import com.jmens.advisor.modules.advisor.service.AdvisorAnalysisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/advisor")
public class AdvisorController {

  private final AdvisorAnalysisService advisorAnalysisService;

  public AdvisorController(AdvisorAnalysisService advisorAnalysisService) {
    this.advisorAnalysisService = advisorAnalysisService;
  }

  @PostMapping("/analyze")
  public Result<ResearchReport> analyze(@Valid @RequestBody AdvisorRequest request) {
    return Result.ok(advisorAnalysisService.analyze(request.query(), request.analysisType()));
  }
}
