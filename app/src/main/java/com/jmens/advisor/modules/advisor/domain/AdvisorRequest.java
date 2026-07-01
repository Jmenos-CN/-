package com.jmens.advisor.modules.advisor.domain;

import jakarta.validation.constraints.NotBlank;

public record AdvisorRequest(
    @NotBlank String query,
    String analysisType
) {}
