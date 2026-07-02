# Stock Advisor Data Sources Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enrich advisor reports with verifiable market context beyond realtime quotes.

**Architecture:** Keep `StockDataPort` as the market-data boundary and implement the next safe vertical slice: real Sina daily K-line fetch, K-line cache, and Agent context expansion. Financial indicators and news are represented as explicit "not configured" context in this slice so the LLM is constrained not to fabricate missing data; real provider adapters remain separate follow-up work after stable endpoints are selected.

**Tech Stack:** Java 21, Spring Boot 4.0.1, Java HttpClient, Jackson, Redis/Redisson optional cache, JUnit 5, MockMvc, Gradle 8.14.

---

## Scope

This phase implements:

- Parse Sina JSONP daily K-line payloads into `KLinePoint`.
- Fetch recent K-line data through `SinaStockDataClient.getRecentKLine`.
- Cache K-line responses through `CachedStockDataPort`.
- Expand `AdvisorAnalysisService` context with K-line trend summary.
- Include a clear no-fabrication line for unconfigured financial and news data.
- Verify with unit tests, full tests, compile, and a smoke request.

This phase does not implement:

- Paid data providers.
- Complex financial statement parsing.
- Real news search/ranking.
- Frontend changes.

## Files

- Modify: `app/src/main/java/com/jmens/advisor/modules/stock/infrastructure/SinaStockDataClient.java`
  - Add K-line URL configuration, JSONP parsing, and HTTP fetch.
- Modify: `app/src/main/java/com/jmens/advisor/modules/stock/service/CachedStockDataPort.java`
  - Cache K-line lists by `stock:kline:{code}:{days}`.
- Modify: `app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisService.java`
  - Fetch K-lines, build trend summary, and inject it into Agent context.
- Test: `app/src/test/java/com/jmens/advisor/modules/stock/infrastructure/SinaStockDataClientTest.java`
  - Cover K-line JSONP parsing and HTTP fetch.
- Test: `app/src/test/java/com/jmens/advisor/modules/stock/service/CachedStockDataPortTest.java`
  - Cover K-line cache hit behavior.
- Test: `app/src/test/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisServiceTest.java`
  - Cover Agent context containing K-line summary and no-fabrication wording.
- Modify docs:
  - `docs/current-state.md`
  - `docs/decisions.md`
  - `docs/runbook.md`

---

## Task 1: Sina K-Line Adapter

- [ ] Write failing tests in `SinaStockDataClientTest`:
  - `parsesSinaKLinePayload`
  - `fetchesRecentKLineFromHttpEndpoint`
- [ ] Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.stock.infrastructure.SinaStockDataClientTest" --console=plain
```

Expected: fail because K-line parsing/fetching is not implemented.

- [ ] Add `parseKLine(String raw, int limit)` and implement `getRecentKLine`.
- [ ] Run the focused test again.
- [ ] Commit:

```powershell
git add app/src/main/java/com/jmens/advisor/modules/stock/infrastructure/SinaStockDataClient.java `
  app/src/test/java/com/jmens/advisor/modules/stock/infrastructure/SinaStockDataClientTest.java
git commit -m "feat: fetch Sina daily kline data"
```

## Task 2: K-Line Cache

- [ ] Write failing test in `CachedStockDataPortTest`:
  - repeated `getRecentKLine(symbol, 5)` should call delegate once.
- [ ] Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.stock.service.CachedStockDataPortTest" --console=plain
```

Expected: fail because K-line cache currently delegates every call.

- [ ] Implement cache read/write using `CacheKey.kline(code, days)` and `ttlProperties.kline()`.
- [ ] Run the focused test again.
- [ ] Commit:

```powershell
git add app/src/main/java/com/jmens/advisor/modules/stock/service/CachedStockDataPort.java `
  app/src/test/java/com/jmens/advisor/modules/stock/service/CachedStockDataPortTest.java
git commit -m "feat: cache stock kline data"
```

## Task 3: Agent Context Enrichment

- [ ] Write failing test in `AdvisorAnalysisServiceTest`:
  - the captured Agent context should contain recent K-line count, latest close, high/low range, and the phrase `financial/news data not configured`.
- [ ] Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.service.AdvisorAnalysisServiceTest" --console=plain
```

Expected: fail because the context only contains quote data.

- [ ] Modify `AdvisorAnalysisService`:
  - call `stockDataPort.getRecentKLine(symbol, 20)`
  - build an ASCII trend summary from K-line data
  - inject the summary into `buildAgentContext`
  - add a no-fabrication line for financial/news data that is not configured
- [ ] Run affected advisor tests.
- [ ] Commit:

```powershell
git add app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisService.java `
  app/src/test/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisServiceTest.java
git commit -m "feat: enrich advisor context with kline data"
```

## Task 4: Verification, Docs, Push

- [ ] Run:

```powershell
.\gradlew.bat :app:test :app:compileJava --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] Start a local smoke service with remote PostgreSQL and Redis optional cache enabled.
- [ ] Call:

```powershell
Invoke-RestMethod -Uri "http://localhost:18080/api/advisor/analyze" `
  -Method Post `
  -ContentType "application/json; charset=utf-8" `
  -Body '{"query":"帮我分析600519","analysisType":"full"}'
```

Expected: `code=200`, `data.stockCode=600519`; logs should not show K-line fetch errors.

- [ ] Update docs to record:
  - K-line data is fetched from Sina JSONP endpoint.
  - K-line cache key is `stock:kline:{code}:{days}`.
  - Financial/news data are explicitly marked not configured in this slice.
- [ ] Commit docs and push the branch.

## Self-Review

- Spec coverage: covers the next executable Phase 4 slice with K-line data and context enrichment.
- Placeholder scan: no task contains an unbounded TODO; financial/news are explicitly out of implementation scope for this slice and constrained in prompt context.
- Type consistency: uses existing `StockDataPort`, `KLinePoint`, `CacheKey.kline`, and `AdvisorAnalysisService` names.
