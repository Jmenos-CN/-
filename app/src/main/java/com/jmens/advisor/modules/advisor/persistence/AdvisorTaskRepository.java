package com.jmens.advisor.modules.advisor.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvisorTaskRepository extends JpaRepository<AdvisorTaskEntity, String> {
}
