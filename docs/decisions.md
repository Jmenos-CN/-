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

## Disable External LLM Calls By Default

Decision: register real LangChain4j Agent runners only when `advisor.llm.enabled=true`.

Reason:

- Unit tests and local startup should not require paid API credentials or network access.
- The production path is still real: enabling the flag creates an OpenAI-compatible `ChatModel`
  and wires five role-specific Agent runners.
- Missing API keys fail fast with a clear configuration error instead of returning silent placeholder output.
