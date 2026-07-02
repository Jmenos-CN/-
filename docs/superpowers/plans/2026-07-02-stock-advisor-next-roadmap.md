# Stock Advisor Next Roadmap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the current LangChain4j A-share advisor MVP into a persistent, cache-aware, asynchronous, and demo-ready investment research assistant.

**Architecture:** Keep the current Spring Boot layered style and evolve it in small vertical slices. The next executable slice is report persistence and history query; Redis cache, Redis Stream, richer data sources, frontend, and deployment are separate follow-up slices with their own implementation plans.

**Tech Stack:** Java 21, Spring Boot 4.0.1, Spring Data JPA, PostgreSQL 17 + pgvector, Redis/Redisson, LangChain4j, JUnit 5, MockMvc, Gradle 8.14.

---

## Scope Check

This roadmap contains multiple independent subsystems:

1. Report persistence and history query.
2. Redis cache for market data and report results.
3. Redis Stream asynchronous analysis tasks.
4. K-line, financial indicator, and news data sources.
5. React frontend for report display.
6. Docker Compose and runbook hardening.

Only Task 1 through Task 6 below are the detailed implementation plan for the next executable slice: **report persistence and history query**. The remaining roadmap stages must each receive their own detailed plan before implementation.

## Roadmap Order

1. **Report persistence and history query**: save every generated `ResearchReport` into PostgreSQL and expose history APIs.
2. **Redis cache**: cache Sina quote responses and repeated report queries to reduce external calls and LLM cost.
3. **Redis Stream async analysis**: return task IDs immediately and generate long-running reports in consumers.
4. **Richer data sources**: add K-line, financial indicator, and news adapters behind ports.
5. **Frontend display**: build a React page for stock input, report tabs, evidence, and history.
6. **Deployment and documentation**: add Docker Compose, environment templates, and interview-oriented architecture docs.

---

## File Structure For Phase 1

- Create: `app/src/main/java/com/jmens/advisor/modules/advisor/persistence/AdvisorReportEntity.java`
  - JPA entity mapped to `stock_advisor_report`.
  - Stores report sections and evidence JSON.
- Create: `app/src/main/java/com/jmens/advisor/modules/advisor/persistence/AdvisorReportRepository.java`
  - Spring Data repository for save and history lookup.
- Create: `app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorReportMapper.java`
  - Converts `ResearchReport` to `AdvisorReportEntity`.
  - Converts `AdvisorReportEntity` back to `ResearchReport`.
- Create: `app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorReportService.java`
  - Transactional persistence and read service.
- Create: `app/src/main/java/com/jmens/advisor/modules/advisor/domain/AdvisorReportSummary.java`
  - Lightweight response for report history lists.
- Modify: `app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisService.java`
  - Persist the generated report after compliance guard processing.
- Modify: `app/src/main/java/com/jmens/advisor/modules/advisor/controller/AdvisorController.java`
  - Add report detail and history endpoints.
- Test: `app/src/test/java/com/jmens/advisor/modules/advisor/service/AdvisorReportServiceTest.java`
  - Tests save, detail read, and stock-code history query.
- Test: `app/src/test/java/com/jmens/advisor/modules/advisor/controller/AdvisorControllerTest.java`
  - Extends MVC contract tests for history APIs.
- Modify: `docs/current-state.md`, `docs/decisions.md`, `docs/runbook.md`
  - Archive persistence behavior and PostgreSQL startup configuration.

---

## Phase 1 Detailed Plan: Report Persistence And History Query

### Task 1: Add Report Persistence Entity And Repository

**Files:**
- Create: `app/src/main/java/com/jmens/advisor/modules/advisor/persistence/AdvisorReportEntity.java`
- Create: `app/src/main/java/com/jmens/advisor/modules/advisor/persistence/AdvisorReportRepository.java`
- Test: `app/src/test/java/com/jmens/advisor/modules/advisor/service/AdvisorReportServiceTest.java`

- [ ] **Step 1: Write a failing repository/service test skeleton**

Create `app/src/test/java/com/jmens/advisor/modules/advisor/service/AdvisorReportServiceTest.java` with this first test:

```java
package com.jmens.advisor.modules.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.modules.advisor.domain.DataEvidence;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({AdvisorReportService.class, AdvisorReportMapper.class})
class AdvisorReportServiceTest {

  @Autowired
  private AdvisorReportService reportService;

  @Test
  void savesAndReadsReportDetail() {
    ResearchReport report = sampleReport();

    Long id = reportService.save(report);

    ResearchReport saved = reportService.getReport(id);
    assertThat(saved.stockCode()).isEqualTo("600519");
    assertThat(saved.stockName()).isEqualTo("贵州茅台");
    assertThat(saved.fundamentalView()).isEqualTo("基本面稳定");
    assertThat(saved.evidences()).singleElement()
        .satisfies(evidence -> assertThat(evidence.source()).isEqualTo("Sina Finance"));
  }

  private ResearchReport sampleReport() {
    return new ResearchReport(
        "600519",
        "贵州茅台",
        LocalDateTime.of(2026, 7, 2, 10, 0),
        "最新价 1200.00，涨跌幅 0.50%",
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
  }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.service.AdvisorReportServiceTest" --console=plain
```

Expected: compile failure because `AdvisorReportService`, `AdvisorReportMapper`, `AdvisorReportEntity`, and `AdvisorReportRepository` do not exist.

- [ ] **Step 3: Create the JPA entity**

Create `app/src/main/java/com/jmens/advisor/modules/advisor/persistence/AdvisorReportEntity.java`:

```java
package com.jmens.advisor.modules.advisor.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Persistent snapshot of one generated stock advisor report.
 */
@Entity
@Table(name = "stock_advisor_report")
public class AdvisorReportEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 16)
  private String stockCode;

  @Column(nullable = false, length = 64)
  private String stockName;

  @Column(nullable = false)
  private LocalDateTime analysisTime;

  @Lob
  @Column(nullable = false)
  private String quoteSummary;

  @Lob
  @Column(nullable = false)
  private String fundamentalView;

  @Lob
  @Column(nullable = false)
  private String technicalView;

  @Lob
  @Column(nullable = false)
  private String valuationView;

  @Lob
  @Column(nullable = false)
  private String newsView;

  @Lob
  @Column(nullable = false)
  private String riskView;

  @Lob
  @Column(nullable = false)
  private String conclusion;

  @Lob
  @Column(nullable = false)
  private String evidencesJson;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getStockCode() {
    return stockCode;
  }

  public void setStockCode(String stockCode) {
    this.stockCode = stockCode;
  }

  public String getStockName() {
    return stockName;
  }

  public void setStockName(String stockName) {
    this.stockName = stockName;
  }

  public LocalDateTime getAnalysisTime() {
    return analysisTime;
  }

  public void setAnalysisTime(LocalDateTime analysisTime) {
    this.analysisTime = analysisTime;
  }

  public String getQuoteSummary() {
    return quoteSummary;
  }

  public void setQuoteSummary(String quoteSummary) {
    this.quoteSummary = quoteSummary;
  }

  public String getFundamentalView() {
    return fundamentalView;
  }

  public void setFundamentalView(String fundamentalView) {
    this.fundamentalView = fundamentalView;
  }

  public String getTechnicalView() {
    return technicalView;
  }

  public void setTechnicalView(String technicalView) {
    this.technicalView = technicalView;
  }

  public String getValuationView() {
    return valuationView;
  }

  public void setValuationView(String valuationView) {
    this.valuationView = valuationView;
  }

  public String getNewsView() {
    return newsView;
  }

  public void setNewsView(String newsView) {
    this.newsView = newsView;
  }

  public String getRiskView() {
    return riskView;
  }

  public void setRiskView(String riskView) {
    this.riskView = riskView;
  }

  public String getConclusion() {
    return conclusion;
  }

  public void setConclusion(String conclusion) {
    this.conclusion = conclusion;
  }

  public String getEvidencesJson() {
    return evidencesJson;
  }

  public void setEvidencesJson(String evidencesJson) {
    this.evidencesJson = evidencesJson;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
```

- [ ] **Step 4: Create the repository**

Create `app/src/main/java/com/jmens/advisor/modules/advisor/persistence/AdvisorReportRepository.java`:

```java
package com.jmens.advisor.modules.advisor.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvisorReportRepository extends JpaRepository<AdvisorReportEntity, Long> {

  List<AdvisorReportEntity> findTop20ByStockCodeOrderByCreatedAtDesc(String stockCode);
}
```

- [ ] **Step 5: Commit entity and repository**

Run:

```powershell
git add app/src/main/java/com/jmens/advisor/modules/advisor/persistence/AdvisorReportEntity.java `
  app/src/main/java/com/jmens/advisor/modules/advisor/persistence/AdvisorReportRepository.java `
  app/src/test/java/com/jmens/advisor/modules/advisor/service/AdvisorReportServiceTest.java
git commit -m "test: define advisor report persistence contract"
```

### Task 2: Add Mapper And Persistence Service

**Files:**
- Create: `app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorReportMapper.java`
- Create: `app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorReportService.java`
- Modify: `app/src/test/java/com/jmens/advisor/modules/advisor/service/AdvisorReportServiceTest.java`

- [ ] **Step 1: Create the mapper**

Create `app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorReportMapper.java`:

```java
package com.jmens.advisor.modules.advisor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jmens.advisor.modules.advisor.domain.DataEvidence;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
import com.jmens.advisor.modules.advisor.persistence.AdvisorReportEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Converts advisor report DTOs to stable database snapshots.
 */
@Component
public class AdvisorReportMapper {

  private final ObjectMapper objectMapper;

  public AdvisorReportMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public AdvisorReportEntity toEntity(ResearchReport report) {
    AdvisorReportEntity entity = new AdvisorReportEntity();
    entity.setStockCode(report.stockCode());
    entity.setStockName(report.stockName());
    entity.setAnalysisTime(report.analysisTime());
    entity.setQuoteSummary(report.quoteSummary());
    entity.setFundamentalView(report.fundamentalView());
    entity.setTechnicalView(report.technicalView());
    entity.setValuationView(report.valuationView());
    entity.setNewsView(report.newsView());
    entity.setRiskView(report.riskView());
    entity.setConclusion(report.conclusion());
    entity.setEvidencesJson(writeEvidences(report.evidences()));
    entity.setCreatedAt(LocalDateTime.now());
    return entity;
  }

  public ResearchReport toReport(AdvisorReportEntity entity) {
    return new ResearchReport(
        entity.getStockCode(),
        entity.getStockName(),
        entity.getAnalysisTime(),
        entity.getQuoteSummary(),
        entity.getFundamentalView(),
        entity.getTechnicalView(),
        entity.getValuationView(),
        entity.getNewsView(),
        entity.getRiskView(),
        entity.getConclusion(),
        readEvidences(entity.getEvidencesJson())
    );
  }

  private String writeEvidences(List<DataEvidence> evidences) {
    try {
      return objectMapper.writeValueAsString(evidences);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize advisor report evidences", exception);
    }
  }

  private List<DataEvidence> readEvidences(String evidencesJson) {
    try {
      return objectMapper.readValue(evidencesJson, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize advisor report evidences", exception);
    }
  }
}
```

- [ ] **Step 2: Create the persistence service**

Create `app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorReportService.java`:

```java
package com.jmens.advisor.modules.advisor.service;

import com.jmens.advisor.modules.advisor.domain.AdvisorReportSummary;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
import com.jmens.advisor.modules.advisor.persistence.AdvisorReportEntity;
import com.jmens.advisor.modules.advisor.persistence.AdvisorReportRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists and reads generated advisor reports.
 */
@Service
public class AdvisorReportService {

  private final AdvisorReportRepository repository;
  private final AdvisorReportMapper mapper;

  public AdvisorReportService(AdvisorReportRepository repository, AdvisorReportMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Transactional
  public Long save(ResearchReport report) {
    AdvisorReportEntity saved = repository.save(mapper.toEntity(report));
    return saved.getId();
  }

  @Transactional(readOnly = true)
  public ResearchReport getReport(Long id) {
    return repository.findById(id)
        .map(mapper::toReport)
        .orElseThrow(() -> new IllegalArgumentException("Advisor report not found: " + id));
  }

  @Transactional(readOnly = true)
  public List<AdvisorReportSummary> findRecentReports(String stockCode) {
    return repository.findTop20ByStockCodeOrderByCreatedAtDesc(stockCode).stream()
        .map(entity -> new AdvisorReportSummary(
            entity.getId(),
            entity.getStockCode(),
            entity.getStockName(),
            entity.getQuoteSummary(),
            entity.getCreatedAt()
        ))
        .toList();
  }
}
```

- [ ] **Step 3: Create the summary DTO**

Create `app/src/main/java/com/jmens/advisor/modules/advisor/domain/AdvisorReportSummary.java`:

```java
package com.jmens.advisor.modules.advisor.domain;

import java.time.LocalDateTime;

public record AdvisorReportSummary(
    Long id,
    String stockCode,
    String stockName,
    String quoteSummary,
    LocalDateTime createdAt
) {}
```

- [ ] **Step 4: Run the service test**

Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.service.AdvisorReportServiceTest" --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit the passing persistence service**

Run:

```powershell
git add app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorReportMapper.java `
  app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorReportService.java `
  app/src/main/java/com/jmens/advisor/modules/advisor/domain/AdvisorReportSummary.java
git commit -m "feat: persist advisor reports"
```

### Task 3: Persist Reports From The Analysis Chain

**Files:**
- Modify: `app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisService.java`
- Modify: `app/src/test/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisServiceTest.java`

- [ ] **Step 1: Extend the existing analysis service test**

Modify `AdvisorAnalysisServiceTest` so it uses a fake `AdvisorReportService` dependency and asserts that one report is saved. Use an in-memory fake class:

```java
private static class CapturingAdvisorReportService extends AdvisorReportService {

  private ResearchReport savedReport;

  CapturingAdvisorReportService() {
    super(null, null);
  }

  @Override
  public Long save(ResearchReport report) {
    this.savedReport = report;
    return 1L;
  }
}
```

Then assert after `service.analyze(...)`:

```java
assertThat(reportService.savedReport.stockCode()).isEqualTo("600519");
assertThat(reportService.savedReport.conclusion()).contains("不构成投资建议");
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.service.AdvisorAnalysisServiceTest" --console=plain
```

Expected: compile failure because `AdvisorAnalysisService` constructor does not accept `AdvisorReportService`.

- [ ] **Step 3: Modify `AdvisorAnalysisService`**

Add a field:

```java
private final AdvisorReportService advisorReportService;
```

Extend the constructor:

```java
public AdvisorAnalysisService(
    StockSymbolParser stockSymbolParser,
    StockDataPort stockDataPort,
    AdvisorWorkflowService advisorWorkflowService,
    ComplianceGuard complianceGuard,
    AdvisorReportService advisorReportService
) {
  this.stockSymbolParser = stockSymbolParser;
  this.stockDataPort = stockDataPort;
  this.advisorWorkflowService = advisorWorkflowService;
  this.complianceGuard = complianceGuard;
  this.advisorReportService = advisorReportService;
}
```

Assign the generated report to a variable and persist it before returning:

```java
ResearchReport report = new ResearchReport(
    quote.code(),
    quote.name(),
    LocalDateTime.now(),
    quoteSummary,
    byRole.getOrDefault(AgentRole.FUNDAMENTAL, ""),
    byRole.getOrDefault(AgentRole.TECHNICAL, ""),
    byRole.getOrDefault(AgentRole.VALUATION, ""),
    byRole.getOrDefault(AgentRole.NEWS, ""),
    byRole.getOrDefault(AgentRole.RISK, ""),
    conclusion,
    List.of(new DataEvidence(
        "Sina Finance",
        quote.name() + "实时行情",
        quoteSummary,
        quote.quoteTime()
    ))
);
advisorReportService.save(report);
return report;
```

- [ ] **Step 4: Update controller and existing tests for the new constructor**

Update all direct `new AdvisorAnalysisService(...)` calls to pass a fake or real `AdvisorReportService`.

In MVC tests, use the same fake pattern:

```java
AdvisorReportService reportService = new CapturingAdvisorReportService();
AdvisorAnalysisService analysisService = new AdvisorAnalysisService(
    new StockSymbolParser(),
    stockDataPort,
    workflowService,
    new ComplianceGuard(),
    reportService
);
```

- [ ] **Step 5: Run affected tests**

Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.service.AdvisorAnalysisServiceTest" `
  --tests "com.jmens.advisor.modules.advisor.controller.AdvisorControllerTest" --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit the integration**

Run:

```powershell
git add app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisService.java `
  app/src/test/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisServiceTest.java `
  app/src/test/java/com/jmens/advisor/modules/advisor/controller/AdvisorControllerTest.java
git commit -m "feat: save generated advisor reports"
```

### Task 4: Add Report History APIs

**Files:**
- Modify: `app/src/main/java/com/jmens/advisor/modules/advisor/controller/AdvisorController.java`
- Modify: `app/src/test/java/com/jmens/advisor/modules/advisor/controller/AdvisorControllerTest.java`

- [ ] **Step 1: Add failing MVC tests**

Extend `AdvisorControllerTest` with:

```java
@Test
void returnsReportDetailById() throws Exception {
  mockMvc.perform(get("/api/advisor/reports/1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.stockCode").value("600519"));
}

@Test
void returnsRecentReportsByStockCode() throws Exception {
  mockMvc.perform(get("/api/advisor/reports").param("stockCode", "600519"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data[0].stockCode").value("600519"));
}
```

- [ ] **Step 2: Run controller tests and verify failure**

Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.controller.AdvisorControllerTest" --console=plain
```

Expected: `404` because the endpoints are not implemented.

- [ ] **Step 3: Modify `AdvisorController` constructor**

Add `AdvisorReportService`:

```java
private final AdvisorReportService advisorReportService;

public AdvisorController(
    AdvisorAnalysisService advisorAnalysisService,
    AdvisorReportService advisorReportService
) {
  this.advisorAnalysisService = advisorAnalysisService;
  this.advisorReportService = advisorReportService;
}
```

- [ ] **Step 4: Add report endpoints**

Add to `AdvisorController`:

```java
@GetMapping("/reports/{id}")
public Result<ResearchReport> getReport(@PathVariable Long id) {
  return Result.success(advisorReportService.getReport(id));
}

@GetMapping("/reports")
public Result<List<AdvisorReportSummary>> findRecentReports(@RequestParam String stockCode) {
  return Result.success(advisorReportService.findRecentReports(stockCode));
}
```

Add imports:

```java
import com.jmens.advisor.modules.advisor.domain.AdvisorReportSummary;
import com.jmens.advisor.modules.advisor.service.AdvisorReportService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
```

- [ ] **Step 5: Run controller tests**

Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.controller.AdvisorControllerTest" --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit the history APIs**

Run:

```powershell
git add app/src/main/java/com/jmens/advisor/modules/advisor/controller/AdvisorController.java `
  app/src/test/java/com/jmens/advisor/modules/advisor/controller/AdvisorControllerTest.java
git commit -m "feat: expose advisor report history APIs"
```

### Task 5: Verify Against Local H2 And Remote PostgreSQL

**Files:**
- Modify: `docs/runbook.md`
- Modify: `docs/current-state.md`
- Modify: `docs/decisions.md`

- [ ] **Step 1: Run full local test suite**

Run:

```powershell
.\gradlew.bat :app:test --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run compile**

Run:

```powershell
.\gradlew.bat :app:compileJava --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Start with remote PostgreSQL in update mode**

Use PowerShell environment variables for the current terminal:

```powershell
$env:SERVER_PORT="18080"
$env:POSTGRES_URL="jdbc:postgresql://192.168.150.101:5432/jmenos_interview_guide"
$env:POSTGRES_USER="postgres"
$env:POSTGRES_PASSWORD="123456"
$env:POSTGRES_DRIVER="org.postgresql.Driver"
.\gradlew.bat :app:bootRun --console=plain
```

Expected startup signals:

- `Tomcat started on port 18080`
- no PostgreSQL connection error
- Hibernate creates or validates `stock_advisor_report`

- [ ] **Step 4: Call analyze API**

In a second PowerShell terminal:

```powershell
Invoke-RestMethod -Uri "http://localhost:18080/api/advisor/analyze" `
  -Method Post `
  -ContentType "application/json; charset=utf-8" `
  -Body '{"query":"帮我分析600519","analysisType":"full"}'
```

Expected:

- `code` is `200`
- `data.stockCode` is `600519`
- `data.conclusion` contains `不构成投资建议`

- [ ] **Step 5: Call history API**

Run:

```powershell
Invoke-RestMethod -Uri "http://localhost:18080/api/advisor/reports?stockCode=600519" `
  -Method Get
```

Expected:

- `code` is `200`
- `data[0].stockCode` is `600519`
- `data[0].id` is not null

- [ ] **Step 6: Update docs**

Update `docs/current-state.md`:

```markdown
- Generated advisor reports are persisted to PostgreSQL table `stock_advisor_report`.
- History APIs are available at `/api/advisor/reports/{id}` and `/api/advisor/reports?stockCode=...`.
```

Update `docs/decisions.md`:

```markdown
## Store Generated Reports As Snapshots

Decision: persist generated advisor reports as immutable snapshots in `stock_advisor_report`.

Reason:

- LLM outputs can change over time, so each generated report should be auditable.
- Snapshot storage supports history review without re-calling expensive LLM providers.
- The table name is project-specific to avoid colliding with existing AI interview platform tables.
```

Update `docs/runbook.md` with the PostgreSQL environment block from Step 3 and the history API examples from Step 5.

- [ ] **Step 7: Commit verification docs**

Run:

```powershell
git add docs/current-state.md docs/decisions.md docs/runbook.md
git commit -m "docs: document advisor report persistence"
```

### Task 6: Final Verification And Push

**Files:**
- No code files unless verification reveals a bug.

- [ ] **Step 1: Run full verification**

Run:

```powershell
.\gradlew.bat :app:test --console=plain
.\gradlew.bat :app:compileJava --console=plain
git status --short --branch
```

Expected:

- both Gradle commands print `BUILD SUCCESSFUL`
- git status shows the current branch and no unstaged/uncommitted files

- [ ] **Step 2: Push the branch**

Run:

```powershell
git push
```

Expected:

- current branch `codex/a-stock-advisor-java-rebuild` is up to date on `origin`

- [ ] **Step 3: Report checkpoint**

Report to the user:

```text
Phase 1 complete: advisor reports are persisted and queryable.
Verified with full tests, compile, and remote PostgreSQL smoke test.
Next recommended phase: Redis cache for Sina quote and repeated report requests.
```

---

## Follow-Up Plans To Write After Phase 1

### Phase 2: Redis Cache

Plan file to create after Phase 1 passes:

`docs/superpowers/plans/2026-07-02-stock-advisor-redis-cache.md`

Scope:

- Re-enable Redisson configuration.
- Add quote cache with short TTL.
- Add report-query cache with query hash.
- Add degraded behavior when Redis is unavailable.
- Verify with unit tests and Redis smoke test against `192.168.150.101:6379`.

### Phase 3: Redis Stream Async Tasks

Plan file to create after Phase 2 passes:

`docs/superpowers/plans/2026-07-02-stock-advisor-async-tasks.md`

Scope:

- Add `stock_advisor_task`.
- Add task creation API.
- Add Redis Stream producer and consumer.
- Add task status query API.
- Verify idempotent consumption and failure status transitions.

### Phase 4: Richer Market Data

Plan file to create after Phase 3 passes:

`docs/superpowers/plans/2026-07-02-stock-advisor-data-sources.md`

Scope:

- Add K-line adapter.
- Add financial indicator adapter.
- Add news adapter.
- Expand Agent context builder.
- Add prompt constraints to prevent fabricated missing data.

### Phase 5: React Frontend

Plan file to create after Phase 4 passes:

`docs/superpowers/plans/2026-07-02-stock-advisor-frontend.md`

Scope:

- Stock input.
- Analysis trigger.
- Report tabs.
- Evidence section.
- History list.
- Manual browser verification.

### Phase 6: Deployment And Interview Docs

Plan file to create after Phase 5 passes:

`docs/superpowers/plans/2026-07-02-stock-advisor-deployment-docs.md`

Scope:

- Docker Compose for backend, PostgreSQL, and Redis.
- `.env.example` without secrets.
- README runbook.
- Interview architecture notes.

---

## Self-Review

- Spec coverage: the plan covers the next executable slice and decomposes remaining subsystems into separate plan files.
- Placeholder scan: the plan contains no unfinished placeholder markers; follow-up phases are intentionally scoped as separate plan files before implementation.
- Type consistency: `ResearchReport`, `DataEvidence`, `AdvisorReportSummary`, `AdvisorReportService`, `AdvisorReportMapper`, `AdvisorReportEntity`, and `AdvisorReportRepository` names are used consistently.
- Scope: the detailed section is limited to report persistence and history query, which can be completed and verified independently.
