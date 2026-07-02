# Stock Advisor Redis Cache Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add optional Redis-backed caching for Sina realtime quotes and repeated advisor report requests.

**Architecture:** Keep Redis disabled by default so local tests and startup do not require infrastructure. When `app.cache.redis.enabled=true`, create a Redisson client and use JSON cache entries; cache failures degrade to the existing direct data/LLM path.

**Tech Stack:** Java 21, Spring Boot 4.0.1, Redisson, Redis, Jackson JavaTimeModule, JUnit 5, Mockito, Gradle 8.14.

---

## Tasks

### Task 1: Cache Abstraction

- Create `CacheClient` with `get` and `put`.
- Create `NoopCacheClient` for default local startup.
- Create `RedissonCacheClient` that catches Redis failures and degrades to cache miss.
- Create `JsonCacheService` for typed JSON serialization.
- Add tests for JSON round-trip and Redis failure fallback.

### Task 2: Redis Configuration

- Create `AdvisorRedisProperties`.
- Create `CacheConfiguration`.
- Add `app.cache.redis.*` and `app.cache.ttl.*` configuration.
- Keep Redisson auto-configuration excluded; this project owns the Redisson client explicitly.

### Task 3: Quote Cache

- Create `CachedStockDataPort` as `@Primary`.
- Delegate to `SinaStockDataClient` on cache miss.
- Cache `StockQuote` by `stock:quote:{code}` using `app.cache.ttl.quote`.
- Add a unit test proving two quote reads only call the delegate once.

### Task 4: Report Cache

- Modify `AdvisorAnalysisService`.
- Cache `ResearchReport` by `advisor:report:{code}:{analysisType}`.
- On report cache hit, return without calling stock data, agents, or persistence.
- Add a service test proving repeated analysis only saves once.

### Task 5: Verification And Docs

- Run full tests and compile.
- Start with remote PostgreSQL and Redis enabled.
- Clear Redis keys for `600519`.
- Call `/api/advisor/analyze` twice and verify only one new DB row is created.
- Update `docs/current-state.md`, `docs/decisions.md`, and `docs/runbook.md`.
- Commit and push.
