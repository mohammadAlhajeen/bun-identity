# Changelog

All notable changes to this project should be documented here.

This project follows a simple changelog format. Add new entries under `Unreleased` until a release tag is created.

## Unreleased

- Simplified the starter around `AppUser` as the only user domain model.
- Kept guest users as direct `AppUser` records with `ROLE_GUEST`.
- Added local registration, login, refresh, logout, profile, and password-change flows.
- Added refresh-token rotation and reuse detection.
- Added OAuth2 user resolution extension points.
- Added Flyway-managed PostgreSQL and H2 schemas.
- Added architecture guardrail tests.
- Added public documentation for API, architecture, configuration, customization, production, contribution, and security.
