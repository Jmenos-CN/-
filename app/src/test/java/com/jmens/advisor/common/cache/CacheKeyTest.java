package com.jmens.advisor.common.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CacheKeyTest {

  @Test
  void buildsStableQuoteKey() {
    assertThat(CacheKey.quote("600519")).isEqualTo("stock:quote:600519");
  }

  @Test
  void buildsStableReportKey() {
    assertThat(CacheKey.report("600519", "full")).isEqualTo("advisor:report:600519:full");
  }
}
