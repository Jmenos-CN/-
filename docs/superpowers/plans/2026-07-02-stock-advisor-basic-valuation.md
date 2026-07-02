# Stock Advisor Basic Valuation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add deterministic PE/PB/ROE-based valuation explanation from existing quote and financial metrics.

**Architecture:** Keep valuation as a lightweight service inside the advisor module. It uses only current price, EPS, BPS, ROE, and debt ratio from already-fetched quote and financial snapshot data. The service returns explanatory text and evidence values; it does not forecast earnings, calculate target price, or produce buy/sell advice.

**Tech Stack:** Java 21, Spring Boot 4.0.1, JUnit 5, AssertJ, Gradle 8.14.

---

## Files

- Create: `app/src/main/java/com/jmens/advisor/modules/advisor/service/BasicValuationService.java`
- Test: `app/src/test/java/com/jmens/advisor/modules/advisor/service/BasicValuationServiceTest.java`
- Modify: `app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisService.java`
- Modify: `app/src/test/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisServiceTest.java`
- Modify: `app/src/test/java/com/jmens/advisor/modules/advisor/controller/AdvisorControllerTest.java`
- Modify docs: `docs/current-state.md`, `docs/decisions.md`, `docs/runbook.md`

## Tasks

### Task 1: Basic Valuation Service

- [ ] Write tests for PE, PB, ROE, and debt-ratio explanation.
- [ ] Write tests for missing EPS/BPS so the service returns unavailable metrics without divide-by-zero.
- [ ] Implement `BasicValuationService`.
- [ ] Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.service.BasicValuationServiceTest" --console=plain
```

### Task 2: Advisor Integration

- [ ] Inject `BasicValuationService` into `AdvisorAnalysisService`.
- [ ] Add `Valuation summary` to Agent context.
- [ ] Use deterministic valuation text as `valuationView` when no valuation Agent output is available.
- [ ] Add `Basic Valuation` evidence with PE/PB/ROE values.
- [ ] Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.service.AdvisorAnalysisServiceTest" `
  --tests "com.jmens.advisor.modules.advisor.controller.AdvisorControllerTest" --console=plain
```

### Task 3: Verification and Docs

- [ ] Run:

```powershell
.\gradlew.bat :app:test :app:compileJava --console=plain
```

- [ ] Smoke `/api/advisor/analyze` with a fresh `analysisType` and verify valuation evidence or valuation view.
- [ ] Update docs.
- [ ] Commit and push `codex/a-stock-advisor-java-rebuild`.

## Self-Review

- Spec coverage: PE/PB/ROE explanation is covered; no forecast model or target price is added.
- Placeholder scan: no unbounded tasks.
- Type consistency: `BasicValuationService` is the only new valuation abstraction.
