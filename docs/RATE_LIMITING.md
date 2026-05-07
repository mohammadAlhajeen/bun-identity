# Rate Limiting Guide

This starter includes application-level rate limiting for public authentication and account-creation endpoints. Keep these limits enabled before exposing the service publicly.

The built-in limiter uses Caffeine in-memory cache entries. It is useful as a default protection layer for one application instance. If you run more than one application instance, add Redis-backed rate limiting or another shared rate-limit store so all instances use the same counters.

## Why Guest Access Needs Limits

`POST /auth/guest` is public and can create a new `ROLE_GUEST` user when the supplied `deviceId` has not been seen before.

Do not rely on `deviceId` as the only limit key:

- The client sends it.
- Attackers can generate new UUIDs.
- A bot can create many guest accounts and refresh-token sessions.
- Guest creation writes to the database.

Guest access should also be limited before requests reach the application or inside the application with shared storage.

## Endpoints To Limit

The built-in limiter protects:

| Endpoint | Default Limit | Risk |
| --- | --- | --- |
| `POST /auth/guest` | 20 requests per hour per client IP | Guest-account and refresh-token session creation |
| `POST /auth/login` | 10 requests per 10 minutes per client IP | Password guessing and credential stuffing |
| `POST /api/user/register` | 5 requests per hour per client IP | Account creation abuse |
| `POST /auth/refresh` | 60 requests per hour per client IP | Token abuse and database load |

## Recommended Placement

Prefer an edge or gateway limit first:

- Cloudflare, Fastly, or another CDN/WAF
- Kubernetes ingress controller
- API gateway
- Load balancer rules
- NGINX or Envoy

Add application-level limits when you need identity-aware behavior. Use Redis or another shared storage backend when you need consistent limits across multiple app instances.

## Built-In Configuration

The default limiter is configured in `application.properties`:

| Property | Default |
| --- | --- |
| `identity.rate-limit.enabled` | `true` |
| `identity.rate-limit.client-ip-header` | `X-Forwarded-For` |
| `identity.rate-limit.max-keys` | `10000` |
| `identity.rate-limit.auth-guest.limit` | `20` |
| `identity.rate-limit.auth-guest.window` | `1h` |
| `identity.rate-limit.auth-login.limit` | `10` |
| `identity.rate-limit.auth-login.window` | `10m` |
| `identity.rate-limit.auth-refresh.limit` | `60` |
| `identity.rate-limit.auth-refresh.window` | `1h` |
| `identity.rate-limit.user-register.limit` | `5` |
| `identity.rate-limit.user-register.window` | `1h` |

The matching environment variables are listed in `example.env`.

The limiter returns HTTP `429 Too Many Requests` with:

- `X-RateLimit-Limit`
- `X-RateLimit-Remaining`
- `Retry-After`
- API error code `RATE_LIMIT_EXCEEDED`

Only trust `X-Forwarded-For` when the application is behind a trusted proxy that overwrites incoming forwarding headers. If clients can send this header directly to the app, configure `identity.rate-limit.client-ip-header` to an empty value and use the remote address, or enforce forwarding headers at the proxy.

## Cache Backend Abstraction

The rate limiter depends on the `RateLimitCache` abstraction. `CaffeineRateLimitCacheConfig` provides the default Caffeine-backed implementation only when no other `RateLimitCache` bean exists.

To replace Caffeine with Redis, implement `RateLimitCache` with Redis atomic counters and register it as a Spring bean. The Caffeine backend will back off automatically.

## Guest Limit Keys

For `/auth/guest`, combine several signals:

- IP address or trusted proxy client IP
- IP subnet for noisy networks
- `deviceId`
- User-Agent family
- Anonymous client fingerprint if your frontend has one

Do not use only `deviceId`.

## Suggested Starting Policy

Tune limits for your product and traffic, but a reasonable starting point is:

| Scope | Example Limit |
| --- | --- |
| Per IP | 20 guest-token requests per hour |
| Per IP | 5 new guest users per 10 minutes |
| Per device ID | 10 guest-token requests per hour |
| Global | Alert on sudden guest creation spikes |

Return HTTP `429 Too Many Requests` when a request exceeds the limit.

## Application-Level Options

The built-in limiter uses Caffeine in-memory counters through the `RateLimitCache` abstraction. If you need shared limits across multiple application instances, add Redis-backed rate limiting or another shared backend. Common choices are:

- Bucket4j with Redis or another shared backend
- Resilience4j rate limiter for single-instance or narrow use cases
- A custom Spring filter backed by Redis counters

For multiple application instances, do not use only Caffeine or other in-memory counters. Each instance would have a separate limit and attackers could bypass limits by spreading traffic.

## Operational Checks

Monitor:

- Guest user creation rate.
- Guest refresh-token session creation rate.
- `429` response rate.
- Requests by IP and subnet.
- Database insert rate for users and refresh tokens.
- Failed login rate.

Create alerts for sudden spikes.

## Client Guidance

Clients should:

- Reuse the same device ID after the first guest token.
- Store refresh tokens securely.
- Refresh tokens instead of repeatedly creating new guest users.
- Handle `429` with backoff.

The server must still enforce limits. Client behavior is not a security boundary.
