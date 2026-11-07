# Migration execution notes

## Completed

### Phase 0 — legacy-java
- Moved `job_processing_platform/`, root `pom.xml`, and design markdown into `legacy-java/`
- Files: `legacy-java/**`

### Phase 1 — foundation
- `package.json`, `tsconfig.json`, `.env.example`, Express `app.ts` / `server.ts`, `config.ts`, health route, CORS, request-id

### Phase 2 — schema
- Prisma schema: `jobs`, `job_state_history`, `rag_embeddings` with `vector(768)`
- Migration SQL under `prisma/migrations/20261107100000_init/` (includes `CREATE EXTENSION vector`)
- Note: local Postgres was unreachable at build time (`P1001`); run `npx prisma migrate deploy` when DB is up

### Phase 3–6 — jobs + worker + retries
- Zod payloads in `src/models/jobPayloads.ts`
- `POST /jobs/new`, list/updates/detail
- Rabbit topology + publish; worker consumers; TTL retry + router + DLQ
- Stub handlers for all 7 types

### Phase 7–8 — RAG + Gemini insights
- Redis log batching → `log.rag.queue` → embeddings (Gemini or hash fallback)
- `POST /api/insights` with Gemini chat + rule fallback

### Phase 9–10 — dashboard + harden
- Ported `public/index.html`
- README, `/health`, Vitest unit tests (payloads + transitions)
- `npx tsc --noEmit` clean; `npm test` 6/6 passing

## Next for human
1. Start Postgres (pgvector), RabbitMQ, Redis
2. `npx prisma migrate deploy && npm run prisma:generate`
3. `npm run dev:api` and `npm run dev:worker`
4. Optional: set `GEMINI_API_KEY`
5. Commit when ready (not committed during this build session)
