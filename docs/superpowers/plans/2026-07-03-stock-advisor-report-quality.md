# Stock Advisor Report Quality Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add explainable report insights and evidence completeness quality scoring to generated stock advisor reports.

**Architecture:** Keep the existing quote, finance, news, valuation, peer comparison, and LangChain4j Agent pipeline unchanged. Add deterministic post-processing services that transform collected `DataEvidence` into `ReportInsight` items and a `ReportQuality` score, then persist and return these fields with `ResearchReport`.

**Tech Stack:** Java 21, Spring Boot, JPA, Jackson, JUnit 5, AssertJ, Gradle.

---

### Task 1: Domain Contract And Failing Tests

**Files:**
- Create: `app/src/test/java/com/jmens/advisor/modules/advisor/service/ReportInsightServiceTest.java`
- Create: `app/src/test/java/com/jmens/advisor/modules/advisor/service/ReportQualityServiceTest.java`
- Modify: `app/src/test/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisServiceTest.java`
- Modify: `app/src/test/java/com/jmens/advisor/modules/advisor/service/AdvisorReportServiceTest.java`

- [x] **Step 1: Write failing tests**

Add tests that expect evidence-to-insight mapping, evidence completeness scoring, report generation fields, and persistence round-trip fields.

- [x] **Step 2: Run focused tests and verify failure**

Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.service.ReportInsightServiceTest" --tests "com.jmens.advisor.modules.advisor.service.ReportQualityServiceTest" --tests "com.jmens.advisor.modules.advisor.service.AdvisorAnalysisServiceTest" --tests "com.jmens.advisor.modules.advisor.service.AdvisorReportServiceTest" --console=plain
```

Expected: compilation fails because `ReportInsight`, `ReportQuality`, `ReportInsightService`, and `ReportQualityService` do not exist.

### Task 2: Deterministic Report Quality Services

**Files:**
- Create: `app/src/main/java/com/jmens/advisor/modules/advisor/domain/ReportInsight.java`
- Create: `app/src/main/java/com/jmens/advisor/modules/advisor/domain/ReportQuality.java`
- Create: `app/src/main/java/com/jmens/advisor/modules/advisor/service/ReportInsightService.java`
- Create: `app/src/main/java/com/jmens/advisor/modules/advisor/service/ReportQualityService.java`
- Modify: `app/src/main/java/com/jmens/advisor/modules/advisor/domain/ResearchReport.java`

- [x] **Step 1: Implement domain records**

Add immutable records for report insights and quality summary. Use compact constructors to normalize null lists and provide an empty quality fallback.

- [x] **Step 2: Implement insight mapping**

Map known evidence sources into stable insight categories: `MARKET`, `NEWS`, `FINANCIAL`, `VALUATION`, and `PEER`.

- [x] **Step 3: Implement quality scoring**

Compute a 0-100 score from five required evidence categories, expose missing evidence types, and add warnings when evidence is incomplete.

- [x] **Step 4: Run service tests**

Run the two new service tests and expect PASS.

### Task 3: Integrate Report Fields Into Analysis And Persistence

**Files:**
- Modify: `app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisService.java`
- Modify: `app/src/main/java/com/jmens/advisor/modules/advisor/persistence/AdvisorReportEntity.java`
- Modify: `app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorReportMapper.java`
- Modify: existing tests that instantiate `ResearchReport`

- [x] **Step 1: Integrate services into analysis**

After `buildEvidences(...)`, build `insights` and `quality`, then return them in `ResearchReport`.

- [x] **Step 2: Persist JSON fields**

Add `insights_json` and `quality_json` text columns to `AdvisorReportEntity`, and serialize/deserialize them in `AdvisorReportMapper`.

- [x] **Step 3: Run focused report tests**

Run focused tests and expect PASS.

### Task 4: Documentation, Verification, Commit, Push

**Files:**
- Modify: `docs/current-state.md`
- Modify: `docs/decisions.md`
- Modify: `docs/runbook.md`

- [x] **Step 1: Update docs**

Document the new report quality stage, JSON persistence fields, test command, and smoke check.

- [x] **Step 2: Run full verification**

Run:

```powershell
.\gradlew.bat :app:compileJava :app:test --console=plain
```

Expected: BUILD SUCCESSFUL.

- [x] **Step 3: Run API smoke test**

Start the backend with local/remote environment variables, call `/api/advisor/analyze`, and verify the JSON response contains non-empty `insights` and `quality`.

- [x] **Step 4: Commit and push**

Commit as `feat: add report quality insights` and push the current branch.
