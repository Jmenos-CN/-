package com.jmens.advisor.modules.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ComplianceGuardTest {

  private final ComplianceGuard guard = new ComplianceGuard();

  @Test
  void removesAbsoluteBuySellInstructionAndAppendsDisclaimer() {
    String cleaned = guard.sanitize("建议立即全仓买入，明天一定上涨。");

    assertThat(cleaned).doesNotContain("全仓买入");
    assertThat(cleaned).doesNotContain("一定上涨");
    assertThat(cleaned).contains("仅供投研参考");
    assertThat(cleaned).contains("不构成投资建议");
  }
}
