# Spring Boot Identity Starter

A production-minded Spring Boot identity starter for projects that need a clean authentication foundation without bringing in a full IAM product on day one.

It includes local registration and login, guest users, JWT access tokens, opaque refresh tokens with rotation, device-aware sessions, optional OAuth2 extension points, PostgreSQL, Flyway migrations, H2 tests, validation, global error responses, and architecture guardrails.

## What This Project Is

This is a reusable backend starter for building your own application identity layer.

Use it when you want:

- Email/password registration and login.
- Guest users that can later become real app users.
- Stateless JWT access tokens.
- Opaque refresh tokens stored as hashes.
- Refresh-token rotation and reuse detection.
- PostgreSQL schema management through Flyway.
- A small feature-first package structure that is easy to customize.

This is not a full IAM platform. It does not try to replace Keycloak, Auth0, Okta, or a dedicated authorization server.

## Stack

- Java 21
- Spring Boot 4
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- H2 for tests
- Maven wrapper

## Quick Start

Requirements:

- Java 21 or newer
- Docker and Docker Compose

Linux/macOS:

```bash
cp example.env .env
bash scripts/generate-jwt-rsa-keys.sh
docker compose up -d
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
Copy-Item example.env .env
.\scripts\generate-jwt-rsa-keys.ps1
docker compose up -d
.\mvnw.cmd spring-boot:run
```

Health check:

```http
GET http://localhost:8080/actuator/health
```

For detailed local setup, troubleshooting, and first requests, see [Setup Guide](docs/SETUP.md).

## First Request

Register a user:

```http
POST /api/user/register
Content-Type: application/json

{
  "username": "alice@example.com",
  "password": "Password1",
  "name": "Alice",
  "phone": "+15551234567",
  "deviceId": "11111111-1111-4111-8111-111111111111"
}
```

Example response:

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "opaque-refresh-token",
  "expiresInSeconds": 3600
}
```

Use the access token for authenticated requests:

```http
Authorization: Bearer <access-token>
```

Guest access is available through `POST /auth/guest`, but it is a public endpoint. Keep the built-in rate limiter enabled and do not use a client-supplied `deviceId` as a trusted limit by itself. See [Rate Limiting Guide](docs/RATE_LIMITING.md).

## Documentation

- [Setup Guide](docs/SETUP.md)
- [API Reference](docs/API.md)
- [Architecture Guide](docs/ARCHITECTURE.md)
- [Configuration Guide](docs/CONFIGURATION.md)
- [Customization Guide](docs/CUSTOMIZATION.md)
- [Production Checklist](docs/PRODUCTION.md)
- [Rate Limiting Guide](docs/RATE_LIMITING.md)
- [Contributing Guide](CONTRIBUTING.md)
- [Security Policy](SECURITY.md)
- [Changelog](CHANGELOG.md)

## Project Layout

```text
src/main/java/com/example/identity
  auth/        Login, JWT, refresh tokens, OAuth2 support, session flows
  user/        AppUser model, registration, guest upgrade, profile flows
  config/      Spring Security and application configuration
  exception/   API error handling
```

The structure is intentionally feature-first. It avoids command/handler scaffolding and global DDD layers because this starter is service-oriented, not domain-heavy.

## Main Endpoints

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| `POST` | `/api/user/register` | Register local user and issue tokens | Public |
| `POST` | `/auth/guest` | Create or reuse guest user and issue tokens | Public |
| `POST` | `/auth/login` | Login with email/password | Public |
| `POST` | `/auth/refresh` | Rotate refresh token and issue access token | Public |
| `POST` | `/auth/logout` | Revoke one refresh token | Public |
| `POST` | `/auth/logout/all` | Revoke all current user's refresh tokens | Bearer |
| `GET` | `/api/user/profile` | Read current user's profile | Bearer |
| `PUT` | `/api/user/profile` | Update current user's profile | Bearer |
| `POST` | `/api/user/change-password` | Change local password and revoke sessions | Bearer |

See [API Reference](docs/API.md) for request and response details.

## Configuration

The application imports `.env` automatically:

```properties
spring.config.import=optional:file:.env[.properties]
```

Start from `example.env`, then replace secrets before deployment.

Important settings are documented in [Configuration Guide](docs/CONFIGURATION.md).

## Tests

Run the full test suite:

```bash
./mvnw test
```

Windows PowerShell:

```powershell
.\mvnw.cmd test
```

The test profile uses H2 in PostgreSQL compatibility mode and Flyway migrations from `src/test/resources/db/migration/h2`.

## Production Notes

Before production:

- Replace the development JWT RSA key pair.
- Use a strong database password.
- Keep `.env` out of git.
- Run behind HTTPS.
- Keep access-token TTL short.
- Review rate limiting for public auth endpoints, especially `POST /auth/guest`.
- Decide whether logout must invalidate access tokens immediately.
- Review the checklist in [Production Checklist](docs/PRODUCTION.md).

## Customize This Template

After cloning:

1. Replace Maven metadata in `pom.xml`.
2. Rename package `com.example.identity` to your real package.
3. Replace `identity-starter` in `application.properties`, `example.env`, and `docker-compose.yml`.
4. Add your own business modules beside `auth` and `user`.
5. Keep identity concepts generic unless your product really needs domain-specific user types.

More guidance is in [Customization Guide](docs/CUSTOMIZATION.md).

## Architecture Policy

This starter intentionally keeps the package layout small:

- Controllers do not use repositories directly.
- Database schema changes live in Flyway migrations.
- `AppUser` is the only user domain model.
- Guest users are direct `AppUser` records with `ROLE_GUEST`.
- The only built-in roles are `ROLE_USER`, `ROLE_GUEST`, and `ROLE_ROOT`.
- No command/handler/application scaffolding is included.

Guardrail tests protect these choices.

## License

MIT License. See [LICENSE](LICENSE).
