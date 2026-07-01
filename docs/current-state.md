# Current State

This project is an independent Java + Spring Boot + LangChain4j rewrite of the previous Python Streamlit A-share advisor prototype.

Implemented scope:

- Spring Boot backend skeleton.
- A-share stock code parsing.
- Typed market data contracts.
- Sina quote payload parser.
- Cache key and TTL policy.
- LangChain4j structured output invoker.
- Parallel advisor workflow using virtual threads.
- Compliance guard for financial-risk wording.
- Advisor analysis API contract.
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
- Redisson auto-configuration is excluded in test resources to keep smoke tests local and deterministic.
- The first milestone intentionally returns an API contract placeholder instead of a full real report.
