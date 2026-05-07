# Architecture Guide

This starter uses a pragmatic service-oriented architecture.

It deliberately avoids global `application/domain/infrastructure` layers because the project is an identity starter, not a large domain model. The current code has real workflows, but it is not domain-heavy enough to justify command handlers, ports, adapters, domain events, or aggregate repositories everywhere.

## Classification

Current complexity: **moderately complex, service-oriented**.

Why it is not simple CRUD:

- Guest users can be reused and upgraded.
- Local and OAuth2 identities can resolve to the same `AppUser`.
- Refresh tokens are rotated and stored only as hashes.
- Refresh-token reuse is treated as a security incident.
- Account state affects authentication.

Why it is not full DDD:

- There are only two main features: `auth` and `user`.
- There is one persistence mechanism.
- Most workflows are direct request-service-repository flows.
- The domain vocabulary is small.
- Extra layers would mostly forward calls.

## Package Structure

```text
com.example.identity
  auth
    controller
    crypto
    dto
    jwt
    oauth2
    repository
    service
    RefreshToken.java

  user
    controller
    dto
    repository
    security
    service
    AppUser.java
    IdentityProvider.java
    Role.java

  config
  exception
```

## Responsibilities

### Controllers

Controllers translate HTTP requests into service calls.

They should:

- Validate request bodies with Jakarta Bean Validation.
- Return HTTP-friendly DTOs.
- Avoid persistence logic.
- Avoid business decisions.

They should not inject repositories directly.

### Services

Services own application workflows.

Important services:

| Service | Responsibility |
| --- | --- |
| `AuthLoginService` | Local login and guest token delegation |
| `TokenIssueService` | Issue access/refresh token pairs |
| `RefreshTokenService` | Refresh-token validation, rotation, reuse detection |
| `LogoutService` | Revoke refresh tokens |
| `UserOnboardingService` | Registration, guest creation/reuse, guest upgrade, OAuth2 user resolution |
| `AppUserService` | Profile, password change, Spring `UserDetailsService` |
| `UserIdentityService` | Resolve current user identity from JWT |

### Entities

Entities are JPA-backed models with some behavior.

`AppUser` represents the real application user. Guest, local, and OAuth2 users are all represented by this one model.

`RefreshToken` represents a refresh-token session record. The plain token is never stored, only its hash.

### Repositories

Repositories are Spring Data JPA interfaces. They are persistence details and should stay behind services.

### Exceptions

The public API error contract is centralized through:

- `ApiException`
- `IdentityException`
- `RefreshTokenReuseException`
- `GlobalExceptionHandler`

## Business Rules

Important rules live in services today:

- Only one `AppUser` model exists.
- Guest users have `ROLE_GUEST`.
- Registered local users have `ROLE_USER`.
- Guest users can be upgraded in place.
- OAuth2 users require verified provider email.
- OAuth2 provider/email conflicts are rejected.
- Refresh tokens are rotated on refresh.
- Reusing a revoked refresh token revokes all sessions and locks the account.

This placement is acceptable for the current size.

## When To Add Stronger Domain Modeling

Do not add DDD layers by default.

Consider stronger domain modeling only if these rules grow:

- More account lifecycle states.
- Multiple identity providers with different linking rules.
- Organization/team membership.
- Permissions beyond simple roles.
- Auditing or compliance workflows.
- Risk scoring, MFA, or recovery flows.
- Multiple persistence or messaging adapters.

If that happens, prefer small tactical improvements first:

```java
appUser.upgradeGuestToLocal(...)
appUser.linkExternalIdentity(...)
appUser.assertCanAuthenticate()
refreshToken.rotateTo(...)
refreshToken.assertUsableForDevice(...)
```

Only introduce full DDD package layers if the domain genuinely becomes large enough.

## Guardrails

Architecture guardrail tests currently protect:

- Feature packages stay simple.
- No customer/seller/admin-specific domain files.
- No command/handler scaffolding.
- Removed YAGNI classes do not return.
- Controllers do not use repositories directly.

These tests are intentionally small. They protect the template from drifting back into a project-specific or over-layered design.

## Known Trade-Offs

### JWT Logout

Logging out revokes refresh tokens. Existing JWT access tokens remain valid until expiry.

This is a standard stateless JWT trade-off. If a product requires immediate access-token invalidation, add token versioning or a blacklist, but that increases state and operational complexity.

### Service-Oriented Workflows

Some domain rules live in services rather than entities. This is pragmatic for the current size. Move behavior into entities only when service logic becomes difficult to test or reason about.

### Single User Model

The starter uses only `AppUser`. Product-specific concepts such as customer, seller, merchant, or admin should be added by the consuming application only when needed.
