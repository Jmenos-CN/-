package com.jmens.advisor.modules.advisor.service;

import java.util.List;

public class ComplianceGuard {

  private static final String DISCLAIMER = "\n\n风险提示：以上内容仅供投研参考，不构成投资建议。市场有风险，决策需谨慎。";
  private static final List<String> BLOCKED_PHRASES = List.of(
      "全仓买入",
      "立即买入",
      "立即卖出",
      "一定上涨",
      "稳赚",
      "必涨"
  );

  public String sanitize(String content) {
    String cleaned = content == null ? "" : content;
    for (String phrase : BLOCKED_PHRASES) {
      cleaned = cleaned.replace(phrase, "[已移除绝对化表述]");
    }
    if (!cleaned.contains("不构成投资建议")) {
      cleaned = cleaned + DISCLAIMER;
    }
    return cleaned;
  }
}
