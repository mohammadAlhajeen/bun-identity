# Configuration Guide

The app reads normal Spring Boot configuration and imports `.env` automatically:

```properties
spring.config.import=optional:file:.env[.properties]
```

Start from:

```bash
cp example.env .env
```

Windows PowerShell:

```powershell
Copy-Item example.env .env
```

## Environment Variables

| Variable | Default | Purpose |
| --- | --- | --- |
| `SPRING_APPLICATION_NAME` | `identity-starter` | Spring application name |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/identity_starter` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `identity` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `identity_password` | Database password |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate` | Keeps Hibernate from modifying schema directly |
| `SPRING_FLYWAY_ENABLED` | `true` | Enables Flyway migrations |
| `SPRING_FLYWAY_LOCATIONS` | `classpath:db/migration/postgresql` | Flyway migration location |
| `SPRING_FLYWAY_BASELINE_ON_MIGRATE` | `false` | Flyway baseline behavior |
| `JWT_RSA_PRIVATE_KEY_PATH` | `file:./keys/jwt-private.pem` | PKCS#8 PEM RSA private-key file used to sign JWTs |
| `JWT_RSA_PUBLIC_KEY_PATH` | `file:./keys/jwt-public.pem` | X.509 PEM RSA public-key file used to verify JWTs |
| `JWT_EXPIRATION_MINUTES` | `60` | Access-token TTL |
| `JWT_ISSUER` | `identity-starter` | JWT issuer claim |

## JWT RSA Key Files

The app reads RSA keys from PEM files. Generate local development keys before running the app:

Windows PowerShell:

```powershell
.\scripts\generate-jwt-rsa-keys.ps1
```

Linux/macOS:

```bash
bash scripts/generate-jwt-rsa-keys.sh
```

Both scripts write:

```text
keys/jwt-private.pem
keys/jwt-public.pem
```

The default `.env` points to those files:

```properties
JWT_RSA_PRIVATE_KEY_PATH=file:./keys/jwt-private.pem
JWT_RSA_PUBLIC_KEY_PATH=file:./keys/jwt-public.pem
```

The `keys/` directory is ignored by git. For production, generate keys outside the repository or mount them from your secret manager, then set these properties to those mounted file locations.

## Database

Local PostgreSQL is provided through `docker-compose.yml`:

```bash
docker compose up -d
```

Default connection:

```text
jdbc:postgresql://localhost:5433/identity_starter
```

The app uses Flyway migrations. Keep:

```properties
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
```

Do not use `update` in production. Schema changes should be explicit migrations.

## Test Profile

Tests use:

```properties
spring.datasource.url=jdbc:h2:mem:identity_starter_test;MODE=PostgreSQL
spring.flyway.locations=classpath:db/migration/h2
```

The H2 migration mirrors the PostgreSQL schema, with database-specific column types adjusted where necessary.

## OAuth2

The starter includes OAuth2 support classes and a Google profile extractor. Projects can enable OAuth2 login in `SecurityConfig` and add provider client registration properties as needed.

Typical Spring properties:

```properties
spring.security.oauth2.client.registration.google.client-id=<client-id>
spring.security.oauth2.client.registration.google.client-secret=<client-secret>
spring.security.oauth2.client.registration.google.scope=openid,email,profile
```

Keep provider secrets outside git.

## Actuator

Exposed actuator endpoints:

```properties
management.endpoints.web.exposure.include=health,info
management.endpoint.health.probes.enabled=true
```

For public deployments, expose only what your infrastructure needs.
