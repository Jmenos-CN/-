package com.jmens.advisor.modules.advisor.domain;

import java.time.LocalDateTime;

public record DataEvidence(
    String source,
    String title,
    String value,
    LocalDateTime fetchedAt
) {}
