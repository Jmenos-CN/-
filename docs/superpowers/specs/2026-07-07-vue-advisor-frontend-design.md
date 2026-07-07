# Vue Advisor Frontend Design

## Goal

Build an independent Vue 3 + Vite frontend for real front-backend integration testing of the Java LangChain4j advisor backend.

## Scope

The first phase uses only existing backend APIs:

- `POST /api/advisor/analyze`
- `GET /api/advisor/reports?stockCode=...`
- `GET /api/advisor/reports/{id}`

The UI must support:

- generating a report from a stock query;
- showing five report sections: fundamental, technical, valuation, news, risk;
- showing `qualityScore`, warnings, `insights`, and `evidences`;
- showing report history and loading detail by ID;
- showing raw JSON for API debugging;
- showing an LLM integration hint based on returned analysis sections;
- reserving a follow-up question area for later report-based Q&A without wiring a backend endpoint yet.

## Out Of Scope

- Login and user management.
- Market overview APIs.
- Redis Stream task polling UI.
- Real follow-up chat API.
- ECharts or complex financial charts.
- Component libraries such as Element Plus.

## Architecture

Create a new `frontend/` directory. The Vue app runs on Vite at `http://localhost:5173`, while the Spring Boot backend runs at `http://localhost:8080`.

Vite proxies `/api` to the backend, so browser requests use same-origin `/api/...` paths during development.

## Layout

The page is a compact advisor workbench:

- left sidebar for project identity, connection hints, and action status;
- main query panel for `query` and `analysisType`;
- report summary panel with stock identity, quality score, response time, and LLM hint;
- report section grid for five Agent views;
- explainability area for insights and evidences;
- right/debug area for report history, follow-up placeholder, and raw JSON.

## Testing

Use Vitest:

- API tests mock `fetch` and verify endpoint paths, methods, request body, and Result unwrapping.
- App tests mount `App.vue` and verify key UI labels, follow-up placeholder, and sync API entry.

## Run Commands

```powershell
cd frontend
pnpm install
pnpm test -- --run
pnpm build
pnpm dev
```
