# Stock Advisor News Source Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a stable, no-key stock news source and inject recent news into advisor context.

**Architecture:** Add a dedicated `StockNewsPort` beside `StockDataPort` so news collection stays independent from quote/K-line data. Use Sina Finance stock news HTML as the first provider because it is server-rendered, no-key, stock-code-addressable, and easy to degrade when unavailable. Cache parsed news lists through the existing `JsonCacheService`; Agent context receives compact titles/links, not full article bodies.

**Tech Stack:** Java 21, Spring Boot 4.0.1, Java HttpClient, Regex HTML extraction, Redis/Redisson optional cache, JUnit 5, Gradle 8.14.

---

## Tasks

### Task 1: Sina News Adapter

- [ ] Add `StockNewsItem` record with `title`, `url`, `publishedAt`, and `source`.
- [ ] Add `StockNewsPort`.
- [ ] Add `SinaStockNewsClient` with `parseNews(String raw, int limit)` and `getRecentNews(StockSymbol symbol, int limit)`.
- [ ] Test parser and HTTP fetch with local `HttpServer`.
- [ ] Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.stock.infrastructure.SinaStockNewsClientTest" --console=plain
```

- [ ] Commit: `feat: fetch Sina stock news`

### Task 2: News Cache

- [ ] Add `CacheKey.news(code, limit)`.
- [ ] Add `CachedStockNewsPort` as `@Primary`.
- [ ] Test repeated news calls hit delegate once.
- [ ] Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.stock.service.CachedStockNewsPortTest" --console=plain
```

- [ ] Commit: `feat: cache stock news`

### Task 3: Advisor Context Integration

- [ ] Inject `StockNewsPort` into `AdvisorAnalysisService`.
- [ ] Fetch recent news safely; news failures should not break quote-based analysis.
- [ ] Add `News summary` to Agent context.
- [ ] Keep financial data marked as not configured so the model does not fabricate financial metrics.
- [ ] Add top news items to report evidence.
- [ ] Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.service.AdvisorAnalysisServiceTest" `
  --tests "com.jmens.advisor.modules.advisor.controller.AdvisorControllerTest" --console=plain
```

- [ ] Commit: `feat: enrich advisor context with stock news`

### Task 4: Verification, Docs, Push

- [ ] Run:

```powershell
.\gradlew.bat :app:test :app:compileJava --console=plain
```

- [ ] Smoke with remote PostgreSQL and Redis using a fresh `analysisType`.
- [ ] Update `docs/current-state.md`, `docs/decisions.md`, and `docs/runbook.md`.
- [ ] Push `codex/a-stock-advisor-java-rebuild`.

## Self-Review

- Spec coverage: implements a real news source, cache, Agent context injection, tests, docs, and smoke verification.
- Placeholder scan: no open-ended implementation tasks; real article-body crawling is explicitly out of scope.
- Type consistency: uses new `StockNewsItem`, `StockNewsPort`, `SinaStockNewsClient`, and `CachedStockNewsPort`.
