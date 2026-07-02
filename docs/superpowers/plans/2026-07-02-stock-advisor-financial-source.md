# Stock Advisor Financial Source Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a stable first financial-indicator source and inject recent financial metrics into advisor reports.

**Architecture:** Add `StockFinancialPort` beside `StockDataPort` and `StockNewsPort` so financial metrics stay independent from quote, K-line, and news collection. Use Eastmoney Data Center `RPT_F10_FINANCE_MAINFINADATA` as the first no-key provider because it returns server-side JSON by `SECUCODE`, including EPS, BPS, revenue, net profit, ROE, and debt ratio. Cache financial snapshots through `JsonCacheService`; Agent context receives compact metrics and keeps the no-fabrication constraint only when financial data are unavailable.

**Tech Stack:** Java 21, Spring Boot 4.0.1, Java HttpClient, Jackson `JsonNode`, Redis/Redisson optional cache, JUnit 5, Gradle 8.14.

---

## Files

- Create: `app/src/main/java/com/jmens/advisor/modules/stock/domain/StockFinancialSnapshot.java`
- Create: `app/src/main/java/com/jmens/advisor/modules/stock/service/StockFinancialPort.java`
- Create: `app/src/main/java/com/jmens/advisor/modules/stock/service/CachedStockFinancialPort.java`
- Create: `app/src/main/java/com/jmens/advisor/modules/stock/infrastructure/EastmoneyFinancialClient.java`
- Test: `app/src/test/java/com/jmens/advisor/modules/stock/infrastructure/EastmoneyFinancialClientTest.java`
- Test: `app/src/test/java/com/jmens/advisor/modules/stock/service/CachedStockFinancialPortTest.java`
- Modify: `app/src/main/java/com/jmens/advisor/common/cache/CacheKey.java`
- Modify: `app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisService.java`
- Modify: advisor tests and docs.

## Tasks

### Task 1: Eastmoney Financial Adapter

- [ ] Write failing parser and HTTP tests for `EastmoneyFinancialClient`.
- [ ] Add `StockFinancialSnapshot` record with stock code/name, report date/type, EPS, BPS, revenue, parent net profit, ROE, and debt ratio.
- [ ] Add `StockFinancialPort`.
- [ ] Implement Eastmoney URL construction and JSON parsing.
- [ ] Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.stock.infrastructure.EastmoneyFinancialClientTest" --console=plain
```

### Task 2: Financial Cache

- [ ] Add `CacheKey.finance(code)`.
- [ ] Add `CachedStockFinancialPort` as `@Primary`.
- [ ] Test repeated calls hit delegate once.
- [ ] Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.stock.service.CachedStockFinancialPortTest" --console=plain
```

### Task 3: Advisor Context Integration

- [ ] Inject `StockFinancialPort` into `AdvisorAnalysisService`.
- [ ] Fetch financial snapshot safely; failures degrade to "not available".
- [ ] Add `Financial summary` to Agent context.
- [ ] Remove the broad "financial data not configured" sentence when metrics are available.
- [ ] Add Eastmoney financial evidence to report evidence.
- [ ] Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.service.AdvisorAnalysisServiceTest" `
  --tests "com.jmens.advisor.modules.advisor.controller.AdvisorControllerTest" --console=plain
```

### Task 4: Verification, Docs, Push

- [ ] Run:

```powershell
.\gradlew.bat :app:test :app:compileJava --console=plain
```

- [ ] Smoke `/api/advisor/analyze` with a fresh `analysisType` and verify Eastmoney financial evidence.
- [ ] Update `docs/current-state.md`, `docs/decisions.md`, and `docs/runbook.md`.
- [ ] Commit and push `codex/a-stock-advisor-java-rebuild`.

## Self-Review

- Spec coverage: adds a real financial provider, cache, Agent context injection, tests, docs, and smoke verification.
- Placeholder scan: no unbounded tasks; second financial provider and complex valuation models are intentionally out of scope.
- Type consistency: uses `StockFinancialSnapshot`, `StockFinancialPort`, `EastmoneyFinancialClient`, and `CachedStockFinancialPort`.
