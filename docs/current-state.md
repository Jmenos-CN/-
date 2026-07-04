# Current State

This project is an independent Java + Spring Boot + LangChain4j rewrite of the previous Python Streamlit A-share advisor prototype.

Implemented scope:

- Spring Boot backend skeleton.
- A-share stock code parsing.
- Typed market data contracts.
- Sina quote payload parser.
- Sina realtime quote HTTP fetch through `StockDataPort`.
- Sina daily K-line JSONP parser and recent K-line fetch through `StockDataPort#getRecentKLine`.
- Sina Finance stock news HTML parser and recent news fetch through `StockNewsPort`.
- Sina news quality enhancement: duplicate filtering, publish-time ranking, and best-effort article summary extraction.
- Eastmoney latest financial indicator JSON parser and recent financial snapshot fetch through `StockFinancialPort`.
- Basic valuation explanation based on latest price, EPS, BPS, ROE, and debt ratio.
- Configured peer-group lookup and peer comparison based on PE, PB, and ROE medians.
- Cache key and TTL policy.
- LangChain4j structured output invoker.
- Parallel advisor workflow using virtual threads.
- Compliance guard for financial-risk wording.
- Advisor analysis API now executes the first real chain:
  `StockSymbolParser -> SinaStockDataClient -> AdvisorWorkflowService -> ResearchReport`.
- LangChain4j AgentRunner abstraction, five role-specific prompt templates, and conditional Spring Bean wiring.
- When `advisor.llm.enabled=true`, the API creates an OpenAI-compatible LangChain4j `ChatModel`,
  registers five Agent runners, and fills the report sections through real LLM calls.
- Generated advisor reports are persisted as snapshots in PostgreSQL table `stock_advisor_report`.
- History APIs are available at `/api/advisor/reports/{id}` and `/api/advisor/reports?stockCode=...`.
- Optional Redis cache is available for Sina realtime quotes and repeated advisor reports.
- When Redis cache is enabled, quote cache keys use `stock:quote:{code}` and report cache keys use
  `advisor:report:{code}:{analysisType}`.
- K-line cache is available with key `stock:kline:{code}:{days}` and default TTL of 1 hour.
- News cache is available with key `stock:news:{code}:{limit}` and default TTL of 30 minutes.
- Financial indicator cache is available with key `stock:finance:{code}` and default TTL of 1 day.
- Advisor Agent context now includes recent K-line, stock-news titles, links, and article summaries when available.
  It also includes latest Eastmoney financial metrics when available. When financial data are unavailable, the prompt
  still explicitly tells the model not to fabricate revenue, profit, valuation, or policy facts.
- Advisor Agent context now includes a deterministic `Valuation summary`; when no LLM valuation Agent output is
  available, `valuationView` falls back to the basic PE/PB/ROE explanation.
- Advisor Agent context now includes a deterministic `Peer comparison summary`; `valuationView` includes peer comparison
  when a configured peer group is available.
- Deterministic quote summary and fallback conclusion use readable Chinese copy with an explicit
  non-investment-advice disclaimer; service tests now protect this contract from mojibake regressions.
- Generated reports now include deterministic explainability metadata:
  `insights` maps evidence sources to MARKET/FINANCIAL/VALUATION/PEER/NEWS explanation items, and `quality` scores
  evidence completeness from 0 to 100 with missing-data warnings.
- Report snapshots persist `evidences_json`, `insights_json`, and `quality_json` so historical reports remain auditable
  without re-running external data calls or LLM Agents.
- A lightweight Spring Boot static report UI is available at `/`. It can generate reports, query report history,
  display `qualityScore` as a quality prompt, expand `insights`, and render the historical report evidence chain.
- The static UI intentionally uses the direct `/api/advisor/analyze` path so the demo stays focused on the Agent
  chain rather than task orchestration.
- Redis Stream async analysis tasks are available:
  `POST /api/advisor/tasks` creates a task, `GET /api/advisor/tasks/{taskId}` reads task state.
- Async task state is persisted in PostgreSQL table `stock_advisor_task`; Redis Stream key `advisor:tasks`
  only carries task IDs.
- Async consumer scheduling is disabled by default and can be enabled with
  `APP_ADVISOR_TASKS_CONSUMER_ENABLED=true`.

Not implemented:

- Broker integration.
- Real trading.
- Portfolio rebalancing.
- Paid data provider integration.
- Secondary news provider integration.
- Full independent React frontend.

Known notes:

- Unit and MVC contract tests do not require a running Redis instance.
- Cache configuration tests inject a mock `RedissonClient` when validating Redis-enabled wiring, so unit tests do not
  depend on remote Redis connectivity.
- Redisson auto-configuration stays excluded; the project creates its own optional Redisson client when
  `APP_CACHE_REDIS_ENABLED=true`.
- Local startup uses an H2 in-memory datasource by default and `ddl-auto=update`, so advisor tables are created
  automatically for local development.
- The current API returns a `ResearchReport` with quote summary, role-based Agent sections,
  compliance-guarded conclusion, and data evidence.
- LLM Agent wiring is disabled by default so local tests and startup do not require external API credentials.
  Set `ADVISOR_LLM_ENABLED=true` and `ADVISOR_LLM_API_KEY` to enable real model-generated sections.
- Remote PostgreSQL `jmenos_interview_guide` is reachable at `192.168.150.101:5432` and already has pgvector enabled.
  Use `SPRING_JPA_HIBERNATE_DDL_AUTO=update` only when intentionally creating or updating advisor tables.
- Remote Redis `192.168.150.101:6379` is reachable and returns `PONG`; Redis cache can be enabled without changing code.
- Redis Stream smoke test against remote PostgreSQL and Redis passed on 2026-07-02:
  a new advisor task moved from `PENDING` to `COMPLETED` and produced a report ID.
- K-line data-source smoke test against remote PostgreSQL and Redis passed on 2026-07-02:
  `/api/advisor/analyze` returned `code=200` for `600519` without K-line fetch errors.
- News data-source smoke test against remote PostgreSQL and Redis passed on 2026-07-02:
  `/api/advisor/analyze` returned `code=200` for `600519` and included `Sina Finance News` evidence items.
- News quality smoke test against remote PostgreSQL and Redis passed on 2026-07-02:
  `/api/advisor/analyze` returned `code=200` for `600519`, included 5 `Sina Finance News` evidence items,
  and logs contained no Sina news request failures.
- Financial data-source smoke test against remote PostgreSQL and Redis passed on 2026-07-02:
  `/api/advisor/analyze` returned `code=200` for `600519`, included one `Eastmoney Financial` evidence item with
  EPS, BPS, revenue, parent net profit, ROE, and debt ratio, and logs contained no Eastmoney financial request failures.
- Basic valuation smoke test against remote PostgreSQL and Redis passed on 2026-07-02:
  `/api/advisor/analyze` returned `code=200` for `600519`, `valuationView` included PE, and evidence included one
  `Basic Valuation` item with PE, PB, ROE, and debt ratio.
- Peer comparison smoke test against remote PostgreSQL and Redis passed on 2026-07-03:
  `/api/advisor/analyze` returned `code=200` for `600519`, `valuationView` included peer comparison, and evidence
  included one `Peer Comparison` item for the configured `liquor` peer group.
- Report quality smoke test against local H2 startup passed on 2026-07-03:
  `/api/advisor/analyze` returned `code=200` for `600519`, response included 9 evidence items, 9 insight items, and
  `data.quality.qualityScore=100`.
- Report UI browser smoke test against local H2 startup passed on 2026-07-03:
  opened `/`, generated `Analyze 600519`, verified the quality card, 9 expandable insights, history list, and 9 evidence
  items in historical report detail.
