# Current State

This project is an independent Java + Spring Boot + LangChain4j rewrite of the previous Python Streamlit A-share advisor prototype.

Implemented scope:

- Spring Boot backend skeleton.
- A-share stock code parsing.
- Typed market data contracts.
- Sina quote payload parser.
- Sina realtime quote HTTP fetch through `StockDataPort`.
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
- React frontend.

Known notes:

- Unit and MVC contract tests do not require a running Redis instance.
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
