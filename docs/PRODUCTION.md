# Production Checklist

This checklist covers the main items to review before deploying a project based on this starter.

## Secrets

- Replace the development JWT RSA key pair.
- Mount JWT RSA key files from your secret manager and set `JWT_RSA_PRIVATE_KEY_PATH` and `JWT_RSA_PUBLIC_KEY_PATH`.
- Use a strong database password.
- Store secrets in your deployment secret manager.
- Keep `.env` out of git.
- Rotate secrets according to your team's policy.

## Transport Security

- Run behind HTTPS.
- Set secure cookie flags if you add cookies.
- Restrict CORS to trusted origins if browser clients call the API.
- Do not expose database ports publicly.

## Tokens

- Keep access-token TTL short enough for your risk profile.
- Treat refresh tokens as credentials.
- Store only refresh-token hashes on the server.
- Revoke refresh tokens on logout and password change.
- Decide whether access tokens must be invalidated immediately after logout.

Current behavior:

- Refresh tokens are revoked on logout.
- Access tokens remain valid until expiry.

If your product needs instant access-token invalidation, add one of:

- Token version claim checked against the database.
- Access-token denylist.
- Shorter access-token TTL.

Each option adds operational cost.

## Database

- Keep `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`.
- Use Flyway for all schema changes.
- Back up the database.
- Test migrations before production.
- Monitor connection pool usage.
- Add indexes for new query paths.

## Logging

- Do not log passwords, refresh tokens, or full JWTs.
- Use structured logs in production if possible.
- Review authentication failure logs for noise and sensitive data.
- Keep security incident logs visible to operations.

## Monitoring

Monitor:

- Login failure rate.
- Refresh-token reuse events.
- Token refresh failures.
- Account lock events.
- HTTP 5xx rate.
- Database latency.
- Flyway migration failures.
- JVM memory and CPU.

## Rate Limiting

This starter includes basic Caffeine-backed in-memory rate limiting for public endpoints through the `RateLimitCache` abstraction. Keep it enabled, and add Redis-backed rate limiting, an edge limiter, a gateway limiter, or another shared-store limiter for production deployments with multiple application instances.

The built-in limits cover:

- `POST /auth/login`
- `POST /api/user/register`
- `POST /auth/guest`
- `POST /auth/refresh`

You can add stronger limits at the edge, API gateway, load balancer, or application level.

Guest access needs special care. `POST /auth/guest` is unauthenticated and can create a new guest user and refresh-token session. Do not trust `deviceId` as the only rate-limit key because clients can generate new values. Combine IP address or trusted proxy client IP, subnet, device ID, and user-agent signals where possible.

Recommended controls:

- Put a limit at the edge or gateway before traffic reaches the app.
- Add Redis-backed or another shared-store `RateLimitCache` implementation if you run multiple app instances.
- Return `429 Too Many Requests` when a client exceeds the limit.
- Monitor guest user creation and refresh-token session creation for spikes.

See [Rate Limiting Guide](RATE_LIMITING.md).

## OAuth2

If OAuth2 is enabled:

- Store client secrets securely.
- Restrict redirect URIs in the provider console.
- Require verified email from providers.
- Handle provider email changes carefully.
- Document supported providers for clients.

## CI

Before merging:

```bash
./mvnw -DskipTests compile
./mvnw test
./mvnw -DskipTests package
```

The included GitHub Actions workflow runs tests and packaging on push and pull request.

## Operational Limits

This starter does not include:

- MFA.
- Password reset.
- Email verification.
- Distributed rate limiting.
- Audit log persistence.
- Admin UI.
- Token introspection.
- Distributed session management.

Add these only when your product needs them.
