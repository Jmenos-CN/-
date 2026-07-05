# A Stock Advisor Demo Guide

## 项目定位

这是一个轻量级 Java + LangChain4j A 股投资分析 Agent 项目。用户输入股票代码或自然语言请求后，系统解析股票标的，抓取实时行情、K 线、新闻、财务指标和同业估值数据，再交给多个角色 Agent 生成结构化研报。

项目主线刻意保持简单：股票解析 -> 数据采集 -> 上下文组装 -> LangChain4j 多角色分析 -> 报告结构化返回 -> 证据链展示。

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

重点检查：

- `data.stockCode` 是否为 `600519`
- `data.quoteSummary` 是否包含实时行情摘要
- `data.valuationView` 是否包含估值解释
- `data.evidences` 是否包含行情、财务、新闻、估值或同业证据
- `data.conclusion` 是否包含非投资建议边界

## 面试讲法

我把项目控制在轻量 Agent 应用范围内，没有继续堆复杂中间件。主链路是：股票解析、数据采集、上下文组装、LangChain4j 多角色分析、报告结构化返回和证据链展示。这样既能体现 Agent 架构，也能让面试官快速理解数据从哪里来、模型在哪里介入、结论为什么可信。

如果被问到为什么不继续强化 Redis Stream 或复杂前端，我会回答：这个项目定位是 Agent 能力展示，不是任务平台。Redis Stream 可以作为可选后端能力保留，但主演示链路应该尽量短，方便验证，也方便讲清楚 Agent 的核心价值。

## 不做什么

- 不做真实交易。
- 不做买卖点预测。
- 不输出确定性投资建议。
- 不把 Redis Stream、任务中心、复杂前端状态作为主线能力。
