# Setup Guide

This guide explains how to run the identity starter locally, verify that it works, and prepare the configuration for your own project.

## Requirements

- Java 21 or newer
- Docker and Docker Compose
- Git
- A terminal that can run the Maven wrapper

The project uses the Maven wrapper, so you do not need to install Maven globally.

## 1. Clone The Project

```bash
git clone <repository-url>
cd identity
```

If you already have the project locally, run the commands from the repository root.

## 2. Create Local Environment And JWT Keys

Linux/macOS:

```bash
cp example.env .env
```

Windows PowerShell:

```powershell
Copy-Item example.env .env
```

The application imports `.env` automatically through:

```properties
spring.config.import=optional:file:.env[.properties]
```

Generate the local JWT RSA key files referenced by `example.env`:

Linux/macOS:

```bash
bash scripts/generate-jwt-rsa-keys.sh
```

Windows PowerShell:

```powershell
.\scripts\generate-jwt-rsa-keys.ps1
```

The scripts write `keys/jwt-private.pem` and `keys/jwt-public.pem`. The `keys/` directory is ignored by git.

The sample values are safe for local development only. Replace secrets and keys before any shared or public deployment.

## 3. Start PostgreSQL

```bash
docker compose up -d
```

This starts PostgreSQL on local port `5433`.

Default local database settings:

| Setting | Value |
| --- | --- |
| Database | `identity_starter` |
| Username | `identity` |
| Password | `identity_password` |
| JDBC URL | `jdbc:postgresql://localhost:5433/identity_starter` |

Check the container:

```bash
docker compose ps
```

## 4. Run Tests

Linux/macOS:

```bash
./mvnw test
```

Windows PowerShell:

```powershell
.\mvnw.cmd test
```

Tests use H2 in PostgreSQL compatibility mode and do not require the Docker database.

## 5. Run The Application

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The API starts on:

```text
http://localhost:8080
```

Health check:

```http
GET http://localhost:8080/actuator/health
```

Expected status:

```json
{
  "status": "UP"
}
```

## 6. Create A User

Register a local user:

```http
POST http://localhost:8080/api/user/register
Content-Type: application/json

{
  "username": "alice@example.com",
  "password": "Password1",
  "name": "Alice",
  "phone": "+15551234567",
  "deviceId": "11111111-1111-4111-8111-111111111111"
}
```

The response contains a JWT access token and an opaque refresh token:

```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "opaque-refresh-token",
  "expiresInSeconds": 3600
}
```

Use the access token on authenticated requests:

```http
Authorization: Bearer <access-token>
```

## 7. Try Guest Access

Guest access creates or reuses a `ROLE_GUEST` user for a device ID:

```http
POST http://localhost:8080/auth/guest
Content-Type: application/json

{
  "deviceId": "22222222-2222-4222-8222-222222222222"
}
```

Important: `/auth/guest` is public. Keep the built-in rate limiter enabled and add an edge or gateway limiter before internet exposure. A client-provided `deviceId` is not enough protection because an attacker can generate unlimited device IDs.

See [Rate Limiting Guide](RATE_LIMITING.md) before enabling guest access in production.

## Common Local Commands

Stop containers:

```bash
docker compose down
```

Stop containers and delete the local PostgreSQL volume:

```bash
docker compose down -v
```

Package the application:

```bash
./mvnw -DskipTests package
```

Windows PowerShell:

```powershell
.\mvnw.cmd -DskipTests package
```

## Common Setup Problems

### Port 5433 Is Already In Use

Change the host port in `docker-compose.yml`:

```yaml
ports:
  - "5434:5432"
```

Then update `.env`:

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5434/identity_starter
```

### Database Connection Fails

Check that PostgreSQL is running:

```bash
docker compose ps
```

Check that `.env` matches `docker-compose.yml`.

### JWT RSA Key Error

Generate the local JWT key files:

```bash
bash scripts/generate-jwt-rsa-keys.sh
```

PowerShell:

```powershell
.\scripts\generate-jwt-rsa-keys.ps1
```

Then confirm `.env` points to the generated files:

```properties
JWT_RSA_PRIVATE_KEY_PATH=file:./keys/jwt-private.pem
JWT_RSA_PUBLIC_KEY_PATH=file:./keys/jwt-public.pem
```

## Before Production

Before deploying a project based on this starter:

- Replace all development secrets.
- Use managed PostgreSQL or a properly operated database.
- Run behind HTTPS.
- Restrict CORS to trusted clients.
- Review rate limiting for login, registration, refresh, and guest-token endpoints.
- Review [Production Checklist](PRODUCTION.md).
