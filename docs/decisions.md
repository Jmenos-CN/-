# Decisions

## Use Java + LangChain4j Instead Of Python + Streamlit

Decision: rebuild the project as a Java backend service.

Reason:

- Better fit for bank software-development interviews.
- Stronger engineering story around API design, caching, async workflow, persistence, and testability.
- LangChain4j keeps the AI Agent implementation inside the Java backend instead of splitting Python services.

## Avoid Direct Buy/Sell Advice

Decision: position the project as an intelligent research assistant, not an investment-advisory or trading system.

Reason:

- Reduces compliance risk.
- Keeps output explainable and evidence-based.
- Fits bank technology roles better than retail stock recommendation.

## Use Virtual Threads For Parallel Agent Execution

Decision: run independent Agent calls through Java virtual threads in the first milestone.

Reason:

- LLM and external data calls are I/O-bound.
- Virtual threads keep the implementation simple without early thread-pool tuning.
- The design is easy to replace with Redis Stream async execution in a later milestone.
