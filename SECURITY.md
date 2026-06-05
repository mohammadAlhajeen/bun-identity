# Security Policy

This project is a starter template. Review and adapt security controls before production use.

## Reporting Security Issues

Do not open a public issue for a suspected vulnerability.

Until a project-specific security contact is configured, report privately to the maintainers of your fork or organization.

When reporting, include:

- A clear description of the issue.
- Steps to reproduce.
- Affected endpoint or component.
- Expected impact.
- Suggested fix, if known.

## Supported Versions

This repository currently tracks the main development line only.

| Version | Supported |
| --- | --- |
| `main` | Yes |
| Older forks | Maintainer responsibility |

## Security Design

Current security choices:

- Passwords are hashed with BCrypt through Spring Security.
- Access tokens are signed JWTs.
- Refresh tokens are opaque random values.
- Refresh tokens are stored only as SHA-256 hashes.
- Refresh tokens are rotated on refresh.
- Reusing a revoked refresh token revokes all sessions and locks the account.
- Database schema is managed by Flyway.

## Important Limitations

The starter does not include:

- Rate limiting.
- MFA.
- Password reset.
- Email verification.
- Device management UI.
- Audit-log persistence.
- Immediate JWT access-token revocation.

Add these based on your product's risk profile.

## Production Requirements

Before production:

- Replace the development JWT RSA key pair.
- Keep JWT private-key files outside the repository.
- Use HTTPS.
- Use strong database credentials.
- Store secrets outside the repository.
- Restrict CORS for browser clients.
- Keep rate limiting enabled for login, registration, guest token, and refresh endpoints.
- Treat `POST /auth/guest` as abuse-sensitive because it is public and can create guest users.
- Do not use client-provided `deviceId` as the only guest rate-limit key.
- Review logs to avoid leaking credentials or tokens.

See [Production Checklist](docs/PRODUCTION.md).
