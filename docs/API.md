# API Reference

This document describes the HTTP API exposed by the starter.

Base URL in local development:

```text
http://localhost:8080
```

Authenticated endpoints require:

```http
Authorization: Bearer <access-token>
```

## Token Response

Successful login-like endpoints return:

```json
{
    "accessToken": "jwt-access-token",
    "refreshToken": "opaque-refresh-token",
    "expiresInSeconds": 3600
}
```

The access token is a JWT. The refresh token is opaque and should be stored securely by the client.

## Error Response

Errors use one response shape:

```json
{
    "timestamp": "2026-05-02T12:00:00Z",
    "status": 400,
    "error": "Bad Request",
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed",
    "path": "/api/user/register",
    "violations": [
        {
            "field": "username",
            "message": "Invalid email format"
        }
    ]
}
```

Common error codes:

| Code                       | Meaning                                             |
| -------------------------- | --------------------------------------------------- |
| `VALIDATION_FAILED`        | Request body or parameter failed validation         |
| `BAD_REQUEST`              | Request body could not be parsed                    |
| `AUTH_UNAUTHORIZED`        | Authentication failed                               |
| `AUTH_FORBIDDEN`           | Authenticated user cannot access the resource       |
| `IDENTITY_UNAUTHORIZED`    | Identity/authentication rule rejected the request   |
| `IDENTITY_FORBIDDEN`       | User account state or identity rule denied access   |
| `IDENTITY_CONFLICT`        | Request conflicts with an existing user or identity |
| `DATA_CONFLICT`            | Database uniqueness conflict                        |
| `AUTH_REFRESH_TOKEN_REUSE` | Revoked refresh token was reused                    |
| `INTERNAL_ERROR`           | Unexpected server error                             |

## Validation Rules

Device IDs must be UUID strings:

```text
11111111-1111-4111-8111-111111111111
```

Passwords must:

- Be at least 8 characters.
- Include at least one uppercase letter.
- Include at least one lowercase letter.
- Include at least one number.

Phone numbers are optional and must match either international format or a local numeric format:

```text
+15551234567
0555123456
```

## Register User

```http
POST /api/user/register
Content-Type: application/json
```

Request:

```json
{
    "username": "alice@example.com",
    "password": "Password1",
    "name": "Alice",
    "phone": "+15551234567",
    "deviceId": "11111111-1111-4111-8111-111111111111"
}
```

Response: `201 Created`

```json
{
    "accessToken": "jwt-access-token",
    "refreshToken": "opaque-refresh-token",
    "expiresInSeconds": 3600
}
```

Behavior:

- Creates a local `AppUser` with `ROLE_USER`.
- If a guest user exists for the same `deviceId`, upgrades that guest user in place.
- Rejects duplicate email addresses.

## Guest Token

```http
POST /auth/guest
Content-Type: application/json
```

Request:

```json
{
    "deviceId": "11111111-1111-4111-8111-111111111111"
}
```

Response: `200 OK`

```json
{
    "accessToken": "jwt-access-token",
    "refreshToken": "opaque-refresh-token",
    "expiresInSeconds": 3600
}
```

Behavior:

- Reuses an existing guest user for the same `deviceId`.
- Creates a new `ROLE_GUEST` user if no guest exists.

Security warning:

- This endpoint is public and can create database records.
- Keep rate limiting enabled before public production use.
- Do not rely on `deviceId` alone as a limit key because the client controls it.
- See [Rate Limiting Guide](RATE_LIMITING.md).

## Login

```http
POST /auth/login
Content-Type: application/json
```

Request:

```json
{
    "username": "alice@example.com",
    "password": "Password1",
    "deviceId": "11111111-1111-4111-8111-111111111111"
}
```

Response: `200 OK`

```json
{
    "accessToken": "jwt-access-token",
    "refreshToken": "opaque-refresh-token",
    "expiresInSeconds": 3600
}
```

Behavior:

- Authenticates with Spring Security.
- Issues a JWT access token.
- Creates a hashed refresh-token row for the device.
- Updates login metadata.

## Refresh Token

```http
POST /auth/refresh
Content-Type: application/json
```

Request:

```json
{
    "refreshToken": "opaque-refresh-token",
    "deviceId": "11111111-1111-4111-8111-111111111111"
}
```

Response: `200 OK`

```json
{
    "accessToken": "new-jwt-access-token",
    "refreshToken": "new-opaque-refresh-token",
    "expiresInSeconds": 3600
}
```

Behavior:

- Hashes the incoming refresh token and loads the token row with a pessimistic write lock.
- Checks device binding when a device ID is present.
- Rejects expired, replaced, or revoked tokens.
- Rotates the token by revoking the old token and creating a new one.
- Detects refresh-token reuse and revokes all sessions for the user.

## Logout One Device

```http
POST /auth/logout
Content-Type: application/json
```

Request:

```json
{
    "refreshToken": "opaque-refresh-token",
    "deviceId": "11111111-1111-4111-8111-111111111111"
}
```

Response: `204 No Content`

Behavior:

- Hashes the refresh token.
- Checks the device binding.
- Revokes the matching refresh token.

## Logout All Devices

```http
POST /auth/logout/all
Authorization: Bearer <access-token>
```

Response: `204 No Content`

Behavior:

- Extracts the current user from the JWT `uid` claim.
- Revokes all refresh tokens for that user.

Important: existing JWT access tokens remain valid until expiry unless you add token versioning or an access-token blacklist.

## Get Profile

```http
GET /api/user/profile
Authorization: Bearer <access-token>
```

Response: `200 OK`

```json
{
    "name": "Alice",
    "phone": "+15551234567",
    "username": "alice@example.com"
}
```

## Update Profile

```http
PUT /api/user/profile
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
    "name": "Alice Smith",
    "phone": "+15557654321"
}
```

Response: `200 OK`

```json
{
    "name": "Alice Smith",
    "phone": "+15557654321",
    "username": "alice@example.com"
}
```

## Change Password

```http
POST /api/user/change-password
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
    "oldPassword": "Password1",
    "newPassword": "Password2"
}
```

Response: `200 OK`

```json
{
    "message": "Password changed successfully"
}
```

Behavior:

- Only works for local users.
- Verifies the old password.
- Requires the new password to be different.
- Revokes all refresh tokens for the user.

## JWT Claims

Access tokens include:

| Claim       | Meaning                             |
| ----------- | ----------------------------------- |
| `iss`       | Configured issuer                   |
| `sub`       | User UUID                           |
| `uid`       | User UUID used by the application   |
| `username`  | User email/username                 |
| `scope`     | Space-separated role names          |
| `device_id` | Device ID passed during token issue |
| `iat`       | Issued at                           |
| `exp`       | Expiration                          |
