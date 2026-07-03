# Stock Advisor Async UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire the static report UI to the async advisor task APIs so report generation creates a task, polls status, and automatically loads the completed report detail.

**Architecture:** Keep backend async APIs unchanged. The page will call `POST /api/advisor/tasks`, then poll `GET /api/advisor/tasks/{taskId}` until `COMPLETED` or `FAILED`; on completion it loads `/api/advisor/reports/{reportId}` and refreshes history. The existing synchronous analyze API remains useful for API smoke tests but is no longer the primary UI generation path.

**Tech Stack:** Java 21, Spring Boot static resources, vanilla JavaScript, MockMvc, JUnit 5, Gradle.

---

### Task 1: Frontend Contract Tests

**Files:**
- Modify: `app/src/test/java/com/jmens/advisor/modules/advisor/controller/StaticFrontendContractTest.java`
- Modify: `app/src/test/java/com/jmens/advisor/modules/advisor/controller/AdvisorControllerTest.java`

- [x] **Step 1: Add failing frontend async contract assertions**

Assert `app.js` contains `createAsyncTask`, `pollTaskUntilDone`, `/api/advisor/tasks`, and `/api/advisor/reports/`.

- [x] **Step 2: Add API task contract assertion**

Assert task creation returns a `taskId`, and task detail returns `status` and `reportId`.

- [x] **Step 3: Run focused tests and verify failure**

Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.controller.StaticFrontendContractTest" --tests "com.jmens.advisor.modules.advisor.controller.AdvisorControllerTest" --console=plain
```

Expected: static frontend contract fails because async UI functions are not implemented yet.

### Task 2: Async UI Implementation

**Files:**
- Modify: `app/src/main/resources/static/index.html`
- Modify: `app/src/main/resources/static/app.js`
- Modify: `app/src/main/resources/static/styles.css`

- [x] **Step 1: Rewrite static text as UTF-8 Chinese**

Replace mojibake text in HTML and JS with readable Chinese labels.

- [x] **Step 2: Create task instead of synchronous analysis from the form**

Change form submission to call `createAsyncTask(query, analysisType)`.

- [x] **Step 3: Poll task status**

Implement `pollTaskUntilDone(taskId)` with bounded polling, visible status updates, and FAILED handling.

- [x] **Step 4: Load completed report detail**

When task status is `COMPLETED`, call `/api/advisor/reports/{reportId}`, render report detail, and refresh history.

### Task 3: Documentation And Verification

**Files:**
- Modify: `docs/current-state.md`
- Modify: `docs/runbook.md`
- Modify: `docs/decisions.md`

- [x] **Step 1: Update docs**

Document that the static UI now uses async task APIs.

- [x] **Step 2: Run full tests**

Run:

```powershell
.\gradlew.bat :app:compileJava :app:test --console=plain
```

- [x] **Step 3: Run API and browser smoke tests**

Start backend, create an async task, poll it to completion, open `/`, generate a report through the UI, and verify quality, insights, history, and evidence render.

### Task 4: Commit And Push

- [x] **Step 1: Stage implementation**
- [x] **Step 2: Commit as `feat: add async report ui flow`**
- [x] **Step 3: Push current branch**
