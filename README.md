# SpecForge

**Turn any OpenAPI spec into actionable artifacts — in seconds.**

SpecForge fetches OpenAPI specifications from GitHub repositories and turns them into structured summaries, mock examples, Postman collections, test skeletons, and breaking-change diffs. Built for API-first teams who want to automate the tedious parts of spec-driven development.

---

## Philosophy

API specifications should be more than reference documents. SpecForge treats OpenAPI specs as **executable blueprints** — a single source of truth from which documentation, examples, tests, and client tooling can be automatically derived.

- **Fetch once, generate many times** — caching ensures you never hit GitHub's rate limits unnecessarily.
- **GitHub-native workflow** — OAuth2 authentication lets you work with private repos directly.
- **Diff before you ship** — detect breaking changes between spec versions before your consumers do.

---

## Features

| Feature | Description |
|---|---|
| **Spec Fetching** | Pull OpenAPI specs from any public or private GitHub repo via the GitHub API. Scans up to 10 common file paths. |
| **Structured Summaries** | Parse and validate specs, extracting endpoints, schemas, auth methods, and metadata. |
| **Mock Examples** | Auto-generate realistic request/response JSON examples for every endpoint using schema inference. |
| **Postman Collection** | Export any spec as a v2.1 Postman collection, ready to import with one click. |
| **Test Skeletons** | Generate compilable JUnit 5 test classes with one test method per endpoint. |
| **Org Scanner** | Scan an entire GitHub organization and find which repos have (or lack) an OpenAPI spec. |
| **Breaking Change Detector** | Diff two spec versions and classify every change as `BREAKING`, `NON_BREAKING`, or `INFO`. |
| **Rate Limiting** | Built-in Bucket4j throttling respects GitHub API limits (4,500 req/h configurable). |
| **Redis Caching** | Parsed summaries, examples, and generated artifacts are cached for fast repeated access. |
| **OAuth2 Login** | Authenticate with your GitHub account to access private repositories. |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 25, Spring Boot 4.1 |
| Web | Spring WebMVC, Spring Security |
| Persistence | PostgreSQL 16, Spring Data JPA, Hibernate |
| Cache | Redis 7, Spring Cache |
| API Parsing | swagger-parser v3, swagger-models-jakarta |
| Rate Limiting | Bucket4j |
| Auth | OAuth2 Client (GitHub) |
| API Docs | SpringDoc + Swagger UI |
| Testing | JUnit 6, Mockito, Spring MockMvc |
| Build | Maven, Docker Compose |

---

## Prerequisites

- **Java 25** or later
- **Docker** and **Docker Compose** (for PostgreSQL and Redis)
- A **GitHub OAuth App** (Client ID + Client Secret)

---

## Quick Start

### 1. Clone and configure

```bash
git clone https://github.com/JesusPartal/specforge.git
cd specforge
```

### 2. Start infrastructure

```bash
docker compose up -d
```

This starts PostgreSQL (port 5432) and Redis (port 6379).

### 3. Configure OAuth2

Create an OAuth App at https://github.com/settings/developers with:
- Homepage URL: `http://localhost:8080`
- Callback URL: `http://localhost:8080/login/oauth2/code/github`

Then export your credentials:

```bash
export GITHUB_CLIENT_ID=your-client-id
export GITHUB_CLIENT_SECRET=your-client-secret
```

### 4. Run

```bash
./mvnw spring-boot:run
```

Visit `http://localhost:8080/swagger-ui` for the interactive API docs.

---

## API Reference

All endpoints require GitHub OAuth2 authentication.

### Specs

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/specs` | List all fetched specs |
| `POST` | `/api/specs` | Create a spec manually |
| `POST` | `/api/specs/fetch?repoUrl=...` | Fetch a spec from a GitHub repo |

### Artifacts

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/specs/{id}/summary` | Parse and summarize a spec |
| `GET` | `/api/specs/{id}/postman` | Generate a Postman collection |
| `GET` | `/api/specs/{id}/examples` | Generate mock request/response examples |
| `GET` | `/api/specs/{id}/tests` | Generate a JUnit test skeleton |

### Analysis

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/specs/scan-org?org=...` | Scan a GitHub org for OpenAPI specs |
| `GET` | `/api/specs/diff?oldId=...&newId=...` | Diff two specs for breaking changes |

---

## Configuration

Key properties in `src/main/resources/application.properties.example`:

```properties
# OAuth2 (set via environment variables)
spring.security.oauth2.client.registration.github.client-id=${GITHUB_CLIENT_ID}
spring.security.oauth2.client.registration.github.client-secret=${GITHUB_CLIENT_SECRET}
spring.security.oauth2.client.registration.github.scope=repo,read:user

# Rate limiting
specforge.rate-limit.capacity=4500
specforge.rate-limit.refill-period-minutes=60
specforge.rate-limit.tokens-per-refill=4500

# Cache TTL (milliseconds)
spring.cache.redis.time-to-live=3600000
```

---

## Architecture

```
src/main/java/com/jesuspartal/specforge/
├── api/               ← REST controllers + DTOs (inbound adapter)
│   ├── controller/
│   └── dto/
├── application/       ← Use cases / business logic
│   └── service/
├── domain/            ← JPA entities (core domain)
│   └── model/
├── infrastructure/    ← Outbound adapters (GitHub API, DB)
│   ├── github/
│   └── repository/
├── config/            ← Spring configuration beans
└── exception/         ← Custom exceptions + global handler
```

Layered architecture inspired by hexagonal / ports-and-adapters, with constructor injection, Java records for DTOs, and `@ControllerAdvice` for RFC 9457 error responses.

---

## Testing

```bash
./mvnw test
```

18 tests covering URL parsing, rate limiting, spec parsing, Postman generation, CRUD operations, controller endpoints, and OAuth2 enforcement.

---

## Roadmap

- [ ] Distributed rate limiting via Redis-backed buckets
- [ ] Push-generated artifacts back to GitHub as PRs
- [ ] Webhook receiver for automated spec validation on push
- [ ] Support for AsyncAPI and GraphQL schemas
- [ ] Multi-language test skeleton output (Python, TypeScript, Go)
