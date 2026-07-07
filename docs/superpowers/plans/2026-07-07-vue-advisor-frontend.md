# Vue Advisor Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add an independent Vue 3 + Vite frontend that can test the Java advisor backend end to end.

**Architecture:** Keep the frontend in `frontend/`, use Vite dev proxy from `/api` to `http://localhost:8080`, and use only existing backend endpoints in the first phase.

**Tech Stack:** Vue 3, TypeScript, Vite, Vitest, Vue Test Utils, native CSS.

---

## Tasks

- [ ] Add frontend package, Vite config, TypeScript config, and Vitest setup.
- [ ] Write failing API client tests for analyze, history, and report detail.
- [ ] Implement typed API client and advisor DTOs.
- [ ] Write failing App contract test for the integration workbench.
- [ ] Implement `App.vue`, `main.ts`, and CSS.
- [ ] Update `docs/runbook.md` and `docs/current-state.md`.
- [ ] Run `pnpm test -- --run`, `pnpm build`, backend compile/tests, and a local smoke check when possible.
