# Job Processing Platform (Node.js / TypeScript)

Async job orchestration with priority RabbitMQ queues, Postgres + pgvector RAG, Redis log batching, and Gemini-powered insights.

The previous Java implementation lives under [`legacy-java/`](legacy-java/) for reference.

## Requirements

- Node.js 20+
- PostgreSQL with `pgvector` (`CREATE EXTENSION vector;`)
- RabbitMQ
- Redis
- Optional: `GEMINI_API_KEY` from [Google AI Studio](https://aistudio.google.com/apikey)

## Setup

```bash
cp .env.example .env
npm install
npx prisma migrate deploy   # or: npx prisma db push
npm run prisma:generate
```

## Run

```bash
npm run dev:api      # HTTP API + dashboard on :3000
npm run dev:worker   # Rabbit consumers (jobs, retries, log RAG)
```

Open `http://localhost:3000` for the dashboard.

## Key endpoints

- `POST /jobs/new` — create job (`type` + validated `data`)
- `GET /jobs` — list / filter / paginate
- `GET /jobs/updates?since=<ISO>` — poll updates
- `GET /jobs/{id}` — job detail
- `POST /api/insights` — AI / rule-based insights
- `GET /health` — health check

## Processes

| Process | Role |
|---|---|
| API (`server.ts`) | REST, publish jobs, log batching produce, static UI |
| Worker (`worker.ts`) | Consume jobs, retries/DLQ, log RAG embeddings |
