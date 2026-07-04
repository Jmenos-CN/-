# Stock Advisor Lightweight Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Polish the A-share advisor into a concise, interview-friendly LangChain4j Agent demo without adding more infrastructure.

**Architecture:** Keep the direct `/api/advisor/analyze` flow as the primary path. Do not introduce new middleware, new persistence models, or new frontend build tools. Improve readable Chinese output, make evidence-based Agent behavior easier to see, and document a short demo path.

**Tech Stack:** Java 21, Spring Boot 4.0.1, LangChain4j, JUnit 5, AssertJ, MockMvc, Spring Boot static resources.

---

## File Structure

- Modify: `app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisService.java`
  - Responsibility: deterministic quote summary and fallback conclusion text.
- Modify: `app/src/test/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisServiceTest.java`
  - Responsibility: service-level contract for readable Chinese report text and no mojibake.
- Modify: `app/src/main/resources/static/index.html`
  - Responsibility: lightweight static demo page labels.
- Modify: `app/src/main/resources/static/app.js`
  - Responsibility: frontend status text and report-section labels.
- Modify: `app/src/test/java/com/jmens/advisor/modules/advisor/controller/StaticFrontendContractTest.java`
  - Responsibility: static UI contract, including readable Chinese copy and synchronous API flow.
- Create: `docs/demo-guide.md`
  - Responsibility: concise local demo script and interview explanation.
- Modify: `docs/current-state.md`
  - Responsibility: archive the new current status after each implementation stage.
- Modify: `docs/runbook.md`
  - Responsibility: link the lightweight demo guide and keep async tasks marked optional.

---

### Task 1: Fix Readable Chinese Report Text

**Files:**
- Modify: `app/src/test/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisServiceTest.java`
- Modify: `app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisService.java`
- Modify: `docs/current-state.md`

- [ ] **Step 1: Write the failing service test**

Add this test method to `AdvisorAnalysisServiceTest`:

```java
@Test
void generatesReadableChineseQuoteSummaryAndConclusion() {
  AdvisorAnalysisService service = service(
      new StubStockDataPort(),
      new StubStockNewsPort(),
      new StubStockFinancialPort(),
      new AdvisorWorkflowService(List.of(
          context -> new SingleAgentAnalysis(AgentRole.FUNDAMENTAL, "基本面稳健，收入和利润数据需要结合最新财报确认。")
      )),
      new CapturingAdvisorReportService()
  );

  ResearchReport report = service.analyze("帮我分析600519", "full");

  assertThat(report.quoteSummary())
      .contains("最新价", "昨收", "涨跌幅", "成交量", "成交额", "行情时间")
      .doesNotContain("鏈", "锛", "閸", "鐢", "璇");
  assertThat(report.conclusion())
      .contains("贵州茅台", "当前行情摘要", "Agent 分析摘要", "不构成投资建议")
      .doesNotContain("鏈", "锛", "閸", "鐢", "璇");
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.service.AdvisorAnalysisServiceTest.generatesReadableChineseQuoteSummaryAndConclusion" --console=plain
```

Expected: FAIL because `buildQuoteSummary` and `buildConclusion` still contain mojibake text.

- [ ] **Step 3: Replace mojibake report text**

In `AdvisorAnalysisService`, replace `buildQuoteSummary` with:

```java
private String buildQuoteSummary(StockQuote quote) {
  return "最新价 %s，昨收 %s，涨跌幅 %s%%，成交量 %d，成交额 %s，行情时间 %s"
      .formatted(
          quote.latestPrice(),
          quote.previousClose(),
          quote.changePercent(),
          quote.volume(),
          quote.amount(),
          quote.quoteTime()
      );
}
```

Replace `buildConclusion` with:

```java
private String buildConclusion(StockQuote quote, List<SingleAgentAnalysis> analyses) {
  String agentSummary = analyses.stream()
      .map(analysis -> analysis.role() + ": " + analysis.content())
      .reduce((left, right) -> left + "\n" + right)
      .orElse("当前未配置分析 Agent，仅返回行情摘要。");
  return """
      %s(%s) 当前行情摘要：%s。
      Agent 分析摘要：
      %s

      以上内容仅用于辅助研究，不构成投资建议。
      """.formatted(quote.name(), quote.code(), buildQuoteSummary(quote), agentSummary);
}
```

- [ ] **Step 4: Run the focused service test**

Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.service.AdvisorAnalysisServiceTest" --console=plain
```

Expected: PASS.

- [ ] **Step 5: Update current-state archive**

Append this bullet under `Implemented scope` in `docs/current-state.md`:

```markdown
- Deterministic quote summary and fallback conclusion now use readable Chinese copy and include an explicit
  non-investment-advice disclaimer.
```

- [ ] **Step 6: Commit**

Run:

```powershell
git add app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisService.java `
  app/src/test/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisServiceTest.java `
  docs/current-state.md
git commit -m "fix: restore readable advisor report copy"
```

---

### Task 2: Restore Readable Static UI Copy

**Files:**
- Modify: `app/src/test/java/com/jmens/advisor/modules/advisor/controller/StaticFrontendContractTest.java`
- Modify: `app/src/main/resources/static/index.html`
- Modify: `app/src/main/resources/static/app.js`
- Modify: `docs/current-state.md`

- [ ] **Step 1: Write the failing static UI contract test**

Add this test method to `StaticFrontendContractTest`:

```java
@Test
void frontendUsesReadableChineseCopyAndDirectAnalyzeFlow() throws IOException {
  String index = read("static/index.html");
  String script = read("static/app.js");

  assertThat(index)
      .contains("研报分析工作台", "分析请求", "生成报告", "历史报告", "核心结论", "证据链")
      .doesNotContain("鐮", "璇", "鍒", "灏", "鎶", "鏆");
  assertThat(script)
      .contains("请输入请求", "分析中", "已生成", "证据质量", "暂无证据链")
      .contains("/api/advisor/analyze")
      .doesNotContain("/api/advisor/tasks")
      .doesNotContain("鐮", "璇", "鍒", "灏", "鎶", "鏆");
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.controller.StaticFrontendContractTest.frontendUsesReadableChineseCopyAndDirectAnalyzeFlow" --console=plain
```

Expected: FAIL because the current static files still contain mojibake strings.

- [ ] **Step 3: Replace `index.html` visible text**

Ensure `app/src/main/resources/static/index.html` contains these exact visible labels:

```html
<h1>研报分析工作台</h1>
<div id="status" class="status">就绪</div>
<label for="query">分析请求</label>
<option value="full">完整分析</option>
<option value="valuation">估值分析</option>
<option value="risk">风险分析</option>
<button type="submit">生成报告</button>
<div class="empty-state">生成或选择一份历史报告后，这里会展示质量评分、洞察和证据链。</div>
<h2>历史报告</h2>
<input id="history-stock-code" type="text" value="600519" aria-label="股票代码">
<button type="submit">查询</button>
<h3>核心结论</h3>
<h3>质量提示</h3>
<h3>可解释洞察</h3>
<h3>证据链</h3>
```

- [ ] **Step 4: Replace `app.js` user-facing text**

Use these exact strings in `app/src/main/resources/static/app.js`:

```javascript
setStatus('请输入请求', 'bad');
setStatus('请输入股票代码', 'bad');
setStatus('分析中', 'warn');
setStatus('已生成', 'good');
setStatus('查询历史', 'warn');
setStatus('历史已更新', 'good');
setStatus('加载详情', 'warn');
setStatus('详情已加载', 'good');
throw new Error(payload.message || '请求失败');
node.querySelector('.stock-code').textContent = `${report.stockCode || '-'} · ${formatTime(report.analysisTime)}`;
node.querySelector('.conclusion').textContent = report.conclusion || '暂无结论';
card.innerHTML = `<span class="quality-score">${score}</span><span>证据质量</span>`;
p.textContent = '核心证据完整，当前报告具备较好的解释基础。';
[...warnings, ...missing.map((type) => `缺少 ${type} 证据`)].forEach((item) => {
return [emptyBlock('暂无可解释洞察')];
return [emptyBlock('暂无证据链')];
historyList.replaceChildren(emptyBlock('暂无历史报告'));
setStatus(event.message || '页面错误', 'bad');
historyList.replaceChildren(emptyBlock('历史报告接口暂不可用'));
```

- [ ] **Step 5: Run the focused frontend contract test**

Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.controller.StaticFrontendContractTest" --console=plain
```

Expected: PASS.

- [ ] **Step 6: Update current-state archive**

Append this bullet under `Implemented scope` in `docs/current-state.md`:

```markdown
- Static report UI copy is readable Chinese and intentionally keeps the direct synchronous Agent demo flow.
```

- [ ] **Step 7: Commit**

Run:

```powershell
git add app/src/main/resources/static/index.html `
  app/src/main/resources/static/app.js `
  app/src/test/java/com/jmens/advisor/modules/advisor/controller/StaticFrontendContractTest.java `
  docs/current-state.md
git commit -m "fix: restore readable advisor ui copy"
```

---

### Task 3: Add A Short Demo Guide

**Files:**
- Create: `docs/demo-guide.md`
- Modify: `docs/runbook.md`
- Modify: `docs/current-state.md`

- [ ] **Step 1: Write the documentation contract check**

Run this before creating the file:

```powershell
Test-Path docs\demo-guide.md
```

Expected: `False`.

- [ ] **Step 2: Create `docs/demo-guide.md`**

Create the file with this content:

```markdown
# A Stock Advisor Demo Guide

## 项目定位

这是一个轻量级 Java + LangChain4j A 股投资分析 Agent 项目。用户输入股票代码或自然语言请求后，系统解析股票标的，抓取实时行情、K 线、新闻、财务指标和同业估值数据，再交给多个角色 Agent 生成结构化研报。

## 推荐演示链路

1. 启动后端：

```powershell
.\gradlew.bat :app:bootRun --console=plain
```

2. 打开静态页面：

```text
http://localhost:8080/
```

3. 输入：

```text
Analyze 600519
```

4. 观察报告内容：

- 核心结论
- 基本面、技术面、估值、新闻、风险五类 Agent 视角
- 质量提示
- 可解释洞察
- 证据链
- 历史报告

## API 烟测

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/advisor/analyze" `
  -Method Post `
  -ContentType "application/json; charset=utf-8" `
  -Body '{"query":"帮我分析600519","analysisType":"full"}'
```

## 面试讲法

我把项目控制在轻量 Agent 应用的范围内，没有继续堆复杂中间件。主链路是：股票解析、数据采集、上下文组装、LangChain4j 多角色分析、报告结构化返回和证据链展示。这样既能体现 Agent 架构，也能让面试官快速理解数据从哪里来、模型在哪里介入、结论为什么可信。

## 不做什么

- 不做真实交易。
- 不做买卖点预测。
- 不输出确定性投资建议。
- 不把 Redis Stream、任务中心、复杂前端状态作为主线能力。
```

- [ ] **Step 3: Link the demo guide from `docs/runbook.md`**

Add this paragraph after the backend startup section:

```markdown
For the shortest end-to-end demo path, see `docs/demo-guide.md`.
```

- [ ] **Step 4: Verify the guide exists and contains key sections**

Run:

```powershell
rg -n "项目定位|推荐演示链路|API 烟测|面试讲法|不做什么" docs\demo-guide.md
```

Expected: all five section titles are printed.

- [ ] **Step 5: Update current-state archive**

Append this bullet under `Implemented scope` in `docs/current-state.md`:

```markdown
- Added `docs/demo-guide.md` as the concise end-to-end demo and interview explanation guide.
```

- [ ] **Step 6: Commit**

Run:

```powershell
git add docs/demo-guide.md docs/runbook.md docs/current-state.md
git commit -m "docs: add lightweight advisor demo guide"
```

---

### Task 4: Final Verification And Push

**Files:**
- No production files changed in this task.

- [ ] **Step 1: Run focused tests**

Run:

```powershell
.\gradlew.bat :app:test --tests "com.jmens.advisor.modules.advisor.service.AdvisorAnalysisServiceTest" --tests "com.jmens.advisor.modules.advisor.controller.StaticFrontendContractTest" --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Compile backend**

Run:

```powershell
.\gradlew.bat :app:compileJava --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Scan for mojibake in touched files**

Run:

```powershell
rg -n "鐮|璇|鍒|灏|鎶|鏆|鏈|锛|閸" app/src/main/resources/static app/src/main/java/com/jmens/advisor/modules/advisor/service/AdvisorAnalysisService.java docs/demo-guide.md
```

Expected: no matches.

- [ ] **Step 4: Push the branch**

Run:

```powershell
git push origin codex/a-stock-advisor-java-rebuild
```

Expected: remote branch updates successfully.

---

## Self-Review

- Spec coverage: the plan keeps the project lightweight, fixes current readability issues, keeps the synchronous Agent demo as the primary path, and adds a concise demo/interview guide.
- Placeholder scan: no task uses open-ended placeholder wording.
- Type consistency: all referenced Java classes and method names already exist in the current codebase.
