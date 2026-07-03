# Stock Advisor Peer Comparison Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add industry/peer comparison so valuation explanations can compare a stock against configured peers.

**Architecture:** Add a small configured peer-group service and a deterministic peer-comparison service. Peer comparison reuses existing `StockDataPort`, `StockFinancialPort`, and `BasicValuationService`; it does not add new data providers, automatic industry classification, target prices, forecasts, or buy/sell advice. Missing peer data degrades to an unavailable summary.

**Tech Stack:** Java 21, Spring Boot 4.0.1, Spring `@ConfigurationProperties`, JUnit 5, AssertJ, Gradle 8.14.

---

## Files

- Create: `app/src/main/java/com/jmens/advisor/modules/advisor/config/PeerGroupProperties.java`
- Create: `app/src/main/java/com/jmens/advisor/modules/advisor/service/PeerGroupService.java`
- Create: `app/src/main/java/com/jmens/advisor/modules/advisor/service/PeerComparisonService.java`
- Test: `app/src/test/java/com/jmens/advisor/modules/advisor/service/PeerGroupServiceTest.java`
- Test: `app/src/test/java/com/jmens/advisor/modules/advisor/service/PeerComparisonServiceTest.java`
- Modify: `app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisService.java`
- Modify: `app/src/test/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisServiceTest.java`
- Modify: `app/src/test/java/com/jmens/advisor/modules/advisor/controller/AdvisorControllerTest.java`
- Modify: `app/src/main/resources/application.yml`
- Modify docs: `docs/current-state.md`, `docs/decisions.md`, `docs/runbook.md`

## Tasks

### Task 1: Peer Group Lookup

- [ ] Write tests proving a stock code resolves configured peers and unknown codes return empty groups.
- [ ] Implement `PeerGroupProperties` and `PeerGroupService`.
- [ ] Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.service.PeerGroupServiceTest" --console=plain
```

### Task 2: Peer Comparison Calculation

- [ ] Write tests for peer PE/PB/ROE median calculation and relative higher/lower/near labels.
- [ ] Write tests for empty peers and missing peer data.
- [ ] Implement `PeerComparisonService`.
- [ ] Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.service.PeerComparisonServiceTest" --console=plain
```

### Task 3: Advisor Integration

- [ ] Inject `PeerComparisonService` into `AdvisorAnalysisService`.
- [ ] Add `Peer comparison summary` to Agent context.
- [ ] Enrich `valuationView` with peer-comparison text.
- [ ] Add `Peer Comparison` evidence.
- [ ] Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.service.AdvisorAnalysisServiceTest" `
  --tests "com.jmens.advisor.modules.advisor.controller.AdvisorControllerTest" --console=plain
```

### Task 4: Verification and Docs

- [ ] Run:

```powershell
.\gradlew.bat :app:test :app:compileJava --console=plain
```

- [ ] Smoke `/api/advisor/analyze` with a fresh `analysisType` and verify peer-comparison evidence when configured peers are reachable.
- [ ] Update docs.
- [ ] Commit and push `codex/a-stock-advisor-java-rebuild`.

## Self-Review

- Spec coverage: implements configured peer groups, peer median comparison, Advisor context/evidence integration, tests, docs, and smoke verification.
- Placeholder scan: no automatic industry classifier or target-price model is hidden in this slice.
- Type consistency: `PeerGroupService`, `PeerComparisonService`, and `PeerGroupProperties` are used consistently.
