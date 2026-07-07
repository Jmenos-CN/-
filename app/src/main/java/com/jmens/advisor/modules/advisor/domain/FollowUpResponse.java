package com.jmens.advisor.modules.advisor.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Answer produced for a follow-up question grounded in one saved report.
 *
 * @param reportId source report ID used as the context boundary
 * @param question original user question
 * @param answer generated answer or deterministic fallback when LLM is disabled
 * @param citedEvidence evidence titles copied from the source report
 * @param contextSources readable source markers used to explain where the answer came from
 * @param llmEnabled whether the response was generated through LangChain4j
 * @param answeredAt response timestamp
 */
public record FollowUpResponse(
    Long reportId,
    String question,
    String answer,
    List<String> citedEvidence,
    List<String> contextSources,
    boolean llmEnabled,
    LocalDateTime answeredAt
) {

  public FollowUpResponse {
    citedEvidence = citedEvidence == null ? List.of() : List.copyOf(citedEvidence);
    contextSources = contextSources == null ? List.of() : List.copyOf(contextSources);
  }
}
