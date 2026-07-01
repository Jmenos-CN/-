package com.jmens.advisor.modules.advisor.controller;

import com.jmens.advisor.common.api.Result;
import com.jmens.advisor.modules.advisor.domain.AdvisorRequest;
import com.jmens.advisor.modules.advisor.service.AdvisorWorkflowService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/advisor")
public class AdvisorController {

  private final AdvisorWorkflowService advisorWorkflowService;

  public AdvisorController(AdvisorWorkflowService advisorWorkflowService) {
    this.advisorWorkflowService = advisorWorkflowService;
  }

  @PostMapping("/analyze")
  public Result<Map<String, Object>> analyze(@Valid @RequestBody AdvisorRequest request) {
    return Result.ok(Map.of(
        "query", request.query(),
        "analysisType", request.analysisType() == null ? "full" : request.analysisType()
    ));
  }
}
