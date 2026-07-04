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
  void frontendUsesReadableChineseCopyAndDirectAnalyzeFlow() throws IOException {
    String index = read("static/index.html");
    String script = read("static/app.js");

    assertThat(index)
        .contains("研报分析工作台", "分析请求", "生成报告", "历史报告", "核心结论", "证据链")
        .doesNotContain("鐮", "璇", "鍒", "灏", "鎶", "鏆");
    assertThat(script)
        .contains("请输入请求", "分析中", "已生成", "证据质量", "暂无证据链")
        .contains("/api/advisor/analyze")
        .doesNotContain("/api/advisor/tasks")
        .doesNotContain("鐮", "璇", "鍒", "灏", "鎶", "鏆");
  }

  private String read(String path) throws IOException {
    return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
  }
}
