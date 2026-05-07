# Customization Guide

This starter is designed to be copied and adapted.

## Rename The Project

Update Maven metadata in `pom.xml`:

- `groupId`
- `artifactId`
- `name`
- `description`
- `url`
- `developers`
- `scm`

Then rename the Java package:

```text
com.example.identity
```

to your real package name.

## Rename Runtime Defaults

Replace `identity-starter` and related names in:

- `src/main/resources/application.properties`
- `example.env`
- `docker-compose.yml`
- `README.md`

## Add Business Modules

Add product-specific modules beside `auth` and `user`:

```text
com.example.identity
  auth
  user
  billing
  orders
  notifications
```

Do not force every module into the same level of architecture. CRUD modules can stay simple. Domain-heavy modules can use stronger modeling when needed.

## Keep AppUser Generic

`AppUser` is the identity model.

Avoid adding project-specific user domains to this starter:

- `Customer`
- `Seller`
- `Merchant`
- `Admin`

Add those in the consuming product only if they represent real business concepts. Usually they should reference `AppUser` instead of replacing it.

Example:

```text
CustomerProfile -> appUserId
MerchantAccount -> appUserId
```

## Roles

Built-in roles:

- `ROLE_USER`
- `ROLE_GUEST`
- `ROLE_ROOT`

Keep starter roles minimal. Add application-specific permissions in the consuming project.

For larger products, consider:

- Role tables.
- Permission tables.
- Organization/team memberships.
- Policy-based authorization.

Do not add those until the product needs them.

## OAuth2 Providers

To support another OAuth2 provider:

1. Add a new implementation of `OAuth2UserProfileExtractor`.
2. Map provider-specific attributes into `ExternalIdentityProfile`.
3. Register the provider in Spring OAuth2 client configuration.

Example class name:

```text
GitHubOAuth2UserProfileExtractor
```

The registry will select the extractor whose `supports(registrationId)` method matches the provider registration ID.

## API Changes

If you change request or response shapes:

1. Update DTOs.
2. Update controller tests.
3. Update `docs/API.md`.
4. Keep backward compatibility where possible.

## Database Changes

Use Flyway migrations for schema changes.

PostgreSQL migrations:

```text
src/main/resources/db/migration/postgresql
```

Test H2 migrations:

```text
src/test/resources/db/migration/h2
```

When adding a production migration, add the matching test migration.

## Architecture Rule

Prefer the smallest structure that keeps behavior clear.

Good default:

```text
feature
  controller
  dto
  repository
  service
  Entity.java
```

Use stronger domain structure only when a feature has real business rules, state transitions, or invariants that services can no longer express cleanly.
