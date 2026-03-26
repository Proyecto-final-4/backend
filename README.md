# Backend PF4

Minimal Spring Boot backend with PostgreSQL + pgvector, Flyway, and Spring AI.

## Quick Start

1. Copy env template:
   - `cp .env.example .env` (Git Bash) or duplicate file manually on Windows.
2. Set a real key in `.env`:
   - `SPRING_AI_OPENAI_API_KEY=...`
3. Start infrastructure:
   - `docker compose up -d`
4. Run app:
   - `./mvnw spring-boot:run` (Linux/macOS)
   - `.\mvnw.cmd spring-boot:run` (Windows)

## Most Used Commands

- **Format code**
  - `./mvnw spotless:apply`
- **Check formatter**
  - `./mvnw spotless:check`
- **Run linter**
  - `./mvnw checkstyle:check`
- **Run SpotBugs**
  - `./mvnw spotbugs:check`
- **Run tests**
  - `./mvnw test`
- **Full local quality gate**
  - `./mvnw -DskipTests spotless:check checkstyle:check spotbugs:check`

Use `.\mvnw.cmd ...` on Windows.

## CI/CD Rules (Short)

- CI is split in two jobs:
  - `quality`: formatter + linter + SpotBugs.
  - `integration-tests`: runs tests with pgvector service.
- `integration-tests` runs only after `quality` passes.
- Any PR/push must pass both jobs.

## Commit Rules

- Enable local hooks:
  - `git config core.hooksPath .githooks`
- `pre-commit` runs formatter/linter checks and blocks OpenAI key leaks.
- `commit-msg` requires Conventional Commit prefixes:
  - `feat`, `fix`, `chore`, `docs`, `test`, `refactor`, etc.
