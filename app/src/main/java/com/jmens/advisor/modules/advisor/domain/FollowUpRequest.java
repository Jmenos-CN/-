package com.jmens.advisor.modules.advisor.domain;

import jakarta.validation.constraints.NotBlank;

/**
 * User question asked against an existing generated advisor report.
 *
 * @param question follow-up question that should be answered from the report context
 */
public record FollowUpRequest(
    @NotBlank(message = "question must not be blank")
    String question
) {}
