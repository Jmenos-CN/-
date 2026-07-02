package com.jmens.advisor.modules.advisor.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvisorReportRepository extends JpaRepository<AdvisorReportEntity, Long> {

  List<AdvisorReportEntity> findTop20ByStockCodeOrderByCreatedAtDesc(String stockCode);
}
