package com.jmens.advisor.modules.advisor.domain;

import com.jmens.advisor.modules.advisor.service.AdvisorTaskStatus;
import java.time.LocalDateTime;

public record AdvisorTaskResponse(
    String taskId,
    String query,
    String analysisType,
    AdvisorTaskStatus status,
    Long reportId,
    String errorMessage,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
