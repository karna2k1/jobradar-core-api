# jobradar-core-api

Core backend of **JobRadar**, an automated job finder: it ingests jobs from public job boards and LinkedIn/Naukri alert emails, scores them against your profile (keywords → embeddings → LLM), emails strong matches, and tracks applications on a kanban board.

This service owns **auth, profile/resume, the kanban tracker and follow-up reminders**, plus the local infrastructure (`infra/`).

## Architecture

```mermaid
flowchart LR
  subgraph FE[Frontends · React + Vite Module Federation]
    shell[jobradar-shell :3000<br/>host · auth · board · profile]
    feed[jobs-feed-mfe :3001]
    ins[insights-mfe :3002]
    shell -- loads --> feed
    shell -- loads --> ins
    feed -. shell/ui .-> shell
    ins -. shell/ui .-> shell
  end

  subgraph BE[Spring Boot services · same JWT secret]
    core[jobradar-core-api :8080<br/>auth · profile · tracker · reminders]
    ingest[job-ingest-service :8081<br/>Greenhouse · Lever · Ashby · Remotive · Adzuna · IMAP alerts]
    match[match-notify-service :8082<br/>keyword · pgvector · LLM · emails]
  end

  kafka[(Redpanda / Kafka)]
  pg[(PostgreSQL 16 + pgvector<br/>one DB per service)]
  redis[(Redis)]
  ai[[Gemini / Groq]]
  mail[[SMTP / MailHog]]

  shell --> core
  feed --> match
  ins --> core & ingest & match
  ingest -- job.ingested --> kafka --> match
  match -- job.matched --> kafka --> core
  match -- /internal/profiles (service JWT) --> core
  match --> ai
  match --> redis
  core & ingest & match --> pg
  core & match --> mail
```

## API

| Endpoint | Purpose |
|---|---|
| `POST /auth/register` `/login` `/refresh` `/logout`, `GET /auth/me` | JWT auth: 15-min access token, 7-day refresh token rotated on every use, reuse detection revokes the whole family |
| `GET/PUT /profile` | Skills, experience, CTC, work mode, target roles, locations, blacklist, alert threshold, quiet hours |
| `POST /profile/resume` | PDF upload → text (PDFBox) → skill extraction merged into the profile |
| `GET/POST /applications`, `PATCH /applications/{id}`, `PATCH /applications/{id}/move`, `DELETE` | Kanban: Suggested → Saved → Applied → Interview → Offer / Rejected; stage history in `application_events` |
| `GET /stats/funnel` | Funnel for insights-mfe |
| `GET /internal/profiles` | Service-to-service (role SERVICE) |

Kafka: consumes `job.matched` → creates *Suggested* cards, moves them on Save, removes them on Ignore. A daily job emails follow-up reminders.

Swagger UI: http://localhost:8080/swagger-ui.html

## Run locally

Prerequisites: Java 21, Maven, Node 22, Docker Desktop. Clone the six repos side by side, then in this repo create `.env` with a 32+ character `JWT_SECRET` (copy the same file to job-ingest-service and match-notify-service).

```powershell
.\infra\run-local.ps1      # infra + 3 backends + 3 frontends → http://localhost:3000
```

Or just infra: `docker compose -f infra/docker-compose.yml up -d` (add `--profile messaging` for Redpanda + MailHog) and `mvn spring-boot:run`.

Tech: Java 21, Spring Boot 3.5, Spring Security, jjwt, JPA/Flyway, Spring Kafka, Spring Mail, PDFBox, Springdoc.
