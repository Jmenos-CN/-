# Stock Advisor Report UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a lightweight report UI that displays report quality, expandable insights, and historical report evidence chains.

**Architecture:** Keep the backend API unchanged and add a Spring Boot static frontend under `app/src/main/resources/static`. The UI calls `/api/advisor/analyze`, `/api/advisor/reports?stockCode=...`, and `/api/advisor/reports/{id}` directly, then renders `quality`, `insights`, and `evidences` from the existing `ResearchReport` JSON.

**Tech Stack:** Java 21, Spring Boot MVC static resources, vanilla HTML/CSS/JavaScript, JUnit 5, MockMvc, Gradle.

---

### Task 1: Failing API And Static Frontend Contract Tests

**Files:**
- Modify: `app/src/test/java/com/jmens/advisor/modules/advisor/controller/AdvisorControllerTest.java`
- Create: `app/src/test/java/com/jmens/advisor/modules/advisor/controller/StaticFrontendContractTest.java`

- [x] **Step 1: Add API response contract assertions**

Assert `/api/advisor/analyze` exposes `data.quality.qualityScore`, `data.insights`, and `data.evidences`, and report detail APIs expose evidence-chain fields for historical report rendering.

- [x] **Step 2: Add static frontend contract test**

Assert `index.html`, `app.js`, and `styles.css` exist under `static`, and the JavaScript contains functions that render report quality, insights, and evidence chains.

- [x] **Step 3: Run focused tests and verify failure**

Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.controller.AdvisorControllerTest" --tests "com.jmens.advisor.modules.advisor.controller.StaticFrontendContractTest" --console=plain
```

Expected: static frontend contract test fails because the static files do not exist yet.

### Task 2: Minimal Static Report UI

**Files:**
- Create: `app/src/main/resources/static/index.html`
- Create: `app/src/main/resources/static/styles.css`
- Create: `app/src/main/resources/static/app.js`

- [x] **Step 1: Implement page shell**

Add a compact work-focused page with analyze form, report list, and report detail panel.

- [x] **Step 2: Implement API calls**

Use `fetch` for analyze, history list, and detail calls. Show loading and error states.

- [x] **Step 3: Render explainability**

Render `qualityScore` as a quality badge, `insights` as expandable details, and `evidences` as source/title/value/time rows.

- [x] **Step 4: Run focused frontend contract tests**

Expected: focused tests pass.

### Task 3: Documentation And Verification

**Files:**
- Modify: `docs/current-state.md`
- Modify: `docs/runbook.md`
- Modify: `docs/decisions.md`

- [x] **Step 1: Update docs**

Document the static report UI, how to open it, and what to inspect during manual联调.

- [x] **Step 2: Run full verification**

Run:

```powershell
.\gradlew.bat :app:compileJava :app:test --console=plain
```

Expected: BUILD SUCCESSFUL.

- [x] **Step 3: Run API smoke test**

Start backend on `18080`, call `/api/advisor/analyze`, then fetch `/api/advisor/reports/{id}` when available.

- [x] **Step 4: Run browser联调**

Open `http://localhost:18080/`, generate a report, confirm quality badge, expandable insights, history detail, and evidence chain render correctly.

### Task 4: Commit And Push

- [ ] **Step 1: Stage implementation**
- [ ] **Step 2: Commit as `feat: add explainable report ui`**
- [ ] **Step 3: Push current branch**
