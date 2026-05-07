# Contributing

Thanks for considering a contribution.

This project is a Spring Boot identity starter. Contributions should keep the project generic, small, and easy to understand.

## Development Setup

Requirements:

- Java 21 or newer
- Docker and Docker Compose

Start local dependencies:

```bash
docker compose up -d
```

Run the app:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

## Test Commands

Run before opening a pull request:

```bash
./mvnw -DskipTests compile
./mvnw test
./mvnw -DskipTests package
```

Windows PowerShell:

```powershell
.\mvnw.cmd -DskipTests compile
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
```

## Contribution Rules

Keep the starter generic:

- Do not add customer, seller, merchant, or admin domains.
- Do not add command/handler scaffolding unless there is a proven need.
- Do not inject repositories into controllers.
- Do not store plain refresh tokens.
- Do not add production secrets to the repository.
- Keep schema changes in Flyway migrations.
- Update documentation when behavior changes.

## Code Style

- Prefer simple services over abstract layers.
- Prefer records for simple DTOs.
- Keep controllers thin.
- Keep validation close to request DTOs.
- Put security-sensitive behavior behind tests.
- Avoid unrelated refactors in focused changes.

## Pull Request Checklist

- Tests pass.
- New behavior has focused tests.
- Public API changes are documented in `docs/API.md`.
- Configuration changes are documented in `docs/CONFIGURATION.md`.
- Production/security implications are documented when relevant.
- The change keeps the project useful as a starter template.
