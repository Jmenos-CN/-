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
- LangChain4j AgentRunner abstraction and five role-specific prompt templates.

Not implemented:

- Broker integration.
- Real trading.
- Portfolio rebalancing.
- Paid data provider integration.
- PostgreSQL report persistence.
- Redis Stream async report generation.
- React frontend.

Known notes:

- Unit and MVC contract tests do not require a running Redis instance.
- Redisson auto-configuration is excluded in the first milestone because no Redis cache adapter is wired yet.
  Re-enable it when implementing the real Redis cache or Redis Stream milestone.
- Local startup uses an H2 in-memory datasource by default because report persistence is not implemented yet.
  Switch `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, and `POSTGRES_DRIVER` when enabling PostgreSQL persistence.
- The current API returns a `ResearchReport` with quote summary, role-based Agent sections,
  compliance-guarded conclusion, and data evidence. If no AgentRunner beans are configured,
  the API still returns a quote-based report with empty Agent sections.
