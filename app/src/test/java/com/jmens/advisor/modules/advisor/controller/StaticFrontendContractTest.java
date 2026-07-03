package com.jmens.advisor.modules.advisor.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class StaticFrontendContractTest {

  @Test
  void exposesStaticReportDashboardFiles() throws IOException {
    assertThat(new ClassPathResource("static/index.html").exists()).isTrue();
    assertThat(new ClassPathResource("static/app.js").exists()).isTrue();
    assertThat(new ClassPathResource("static/styles.css").exists()).isTrue();
  }

  @Test
  void frontendContainsExplainabilityRenderingHooks() throws IOException {
    String index = read("static/index.html");
    String script = read("static/app.js");

    assertThat(index)
        .contains("advisor-form")
        .contains("quality-card")
        .contains("insight-list")
        .contains("evidence-list");
    assertThat(script)
        .contains("renderQuality")
        .contains("renderInsights")
        .contains("renderEvidences")
        .contains("/api/advisor/analyze")
        .contains("/api/advisor/reports");
  }


  @Test
  void frontendContainsAsyncTaskFlowHooks() throws IOException {
    String index = read("static/index.html");
    String script = read("static/app.js");

    assertThat(index)
        .contains("task-card")
        .contains("task-status")
        .contains("task-id");
    assertThat(script)
        .contains("createAsyncTask")
        .contains("pollTaskUntilDone")
        .contains("loadCompletedReport")
        .contains("/api/advisor/tasks")
        .contains("/api/advisor/reports/");
  }

  private String read(String path) throws IOException {
    return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
  }
}
