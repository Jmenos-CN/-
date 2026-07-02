# Stock Advisor News Quality Enhancement Plan

> **For agentic workers:** Use `superpowers:executing-plans` to execute this plan task-by-task.

**Goal:** Improve the existing Sina news integration with article-summary extraction, duplicate filtering, and stable ranking before adding more providers.

**Architecture:** Keep `StockNewsPort` unchanged as the boundary. Extend `StockNewsItem` with an optional summary while preserving the current four-argument constructor. `SinaStockNewsClient` still owns Sina-specific HTML parsing and will enrich list items by fetching article pages best-effort. Advisor context should include compact summaries, and failures must degrade to title-only news.

**Scope:**

- Add best-effort article body summary extraction from Sina article HTML.
- Deduplicate by normalized title and URL.
- Sort by `publishedAt` descending before applying the requested limit.
- Keep external article fetch failures non-fatal.
- Do not add a second news provider in this slice.

## Tasks

### Task 1: Parser Tests

- [ ] Add tests for duplicate removal and time-desc sorting.
- [ ] Add tests for article body summary extraction from local HTTP article pages.
- [ ] Add tests proving failed article fetch still returns title-only news.

### Task 2: Implementation

- [ ] Extend `StockNewsItem` with optional `summary`.
- [ ] Implement `parseArticleSummary(String raw)`.
- [ ] Fetch article pages best-effort in `SinaStockNewsClient`.
- [ ] Deduplicate and sort before limiting.

### Task 3: Advisor Context

- [ ] Include summaries in `News summary` when available.
- [ ] Keep evidence title and URL stable for frontend compatibility.

### Task 4: Verification and Docs

- [ ] Run focused news and advisor tests.
- [ ] Run full backend test/compile verification.
- [ ] Update `docs/current-state.md`, `docs/decisions.md`, and `docs/runbook.md`.
