# Stock Advisor Async Tasks Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Redis Stream-backed asynchronous advisor analysis tasks.

**Architecture:** Persist task state in PostgreSQL and use Redis Stream only as the delivery queue. The first version supports one application consumer loop and idempotent status transitions; Redis is enabled explicitly with configuration.

**Tech Stack:** Java 21, Spring Boot 4.0.1, Spring Data JPA, PostgreSQL, Redisson RStream, Redis Stream, JUnit 5, MockMvc.

---

## Tasks

### Task 1: Task Persistence

- Add `AdvisorTaskStatus`.
- Add `AdvisorTaskEntity` mapped to `stock_advisor_task`.
- Add `AdvisorTaskRepository`.
- Add `AdvisorTaskResponse`.
- Add `AdvisorTaskService` to create tasks and update status.

### Task 2: Stream Queue Port

- Add `AdvisorTaskQueue`, `AdvisorTaskMessage`, and `InMemoryAdvisorTaskQueue`.
- Add `RedissonAdvisorTaskQueue` using `RStream`.
- Add queue configuration that chooses Redisson when a `RedissonClient` exists.

### Task 3: Worker

- Add `AdvisorTaskWorker.processNext()`.
- Worker reads one stream message, marks task `PROCESSING`, runs `AdvisorAnalysisService`, stores report id, marks `COMPLETED`, and acknowledges the message.
- On exception, mark `FAILED` and acknowledge to avoid endless hot-loop retries in the MVP.

### Task 4: Controller APIs

- Add `POST /api/advisor/tasks`.
- Add `GET /api/advisor/tasks/{id}`.
- Keep synchronous `/api/advisor/analyze` unchanged.

### Task 5: Verification

- Run focused tests, full tests, and compile.
- Start with remote PostgreSQL and Redis enabled.
- Create async task, process it through the worker, verify status is `COMPLETED`.
- Update docs and push.
