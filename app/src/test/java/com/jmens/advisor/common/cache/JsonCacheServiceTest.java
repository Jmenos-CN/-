package com.jmens.advisor.common.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.modules.advisor.domain.DataEvidence;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonCacheServiceTest {

  @Test
  void storesAndReadsTypedValueAsJson() {
    InMemoryCacheClient cacheClient = new InMemoryCacheClient();
    JsonCacheService cacheService = new JsonCacheService(cacheClient);
    ResearchReport report = new ResearchReport(
        "600519",
        "贵州茅台",
        LocalDateTime.of(2026, 7, 2, 10, 0),
        "最新价 1200.00",
        "基本面稳定",
        "技术面震荡",
        "估值数据不足",
        "新闻数据暂缺",
        "需关注波动风险，不构成投资建议",
        "综合分析仅供投研参考，不构成投资建议。",
        List.of(new DataEvidence(
            "Sina Finance",
            "贵州茅台实时行情",
            "最新价 1200.00",
            LocalDateTime.of(2026, 7, 2, 10, 0)
        ))
    );

    cacheService.put("advisor:report:600519:full", report, Duration.ofMinutes(5));

    assertThat(cacheService.get("advisor:report:600519:full", ResearchReport.class))
        .hasValueSatisfying(cached -> {
          assertThat(cached.stockCode()).isEqualTo("600519");
          assertThat(cached.evidences()).singleElement()
              .satisfies(evidence -> assertThat(evidence.fetchedAt())
                  .isEqualTo(LocalDateTime.of(2026, 7, 2, 10, 0)));
        });
  }
}
