# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run
./gradlew bootRun

# Test all
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.zipzabe.SomeTest"

# Clean build
./gradlew clean build
```

## Environment Variables

The application requires these environment variables (set in `.env` or shell):

| Variable | Purpose |
|---|---|
| `MYSQL_URL` | JDBC URL for MySQL |
| `MYSQL_USERNAME` | DB username |
| `MYSQL_PASSWORD` | DB password |
| `KAKAO_CLIENT` | Kakao OAuth2 client ID |
| `KAKAO_SECRET` | Kakao OAuth2 client secret |
| `KAKAO_REDIRECT_URI` | Kakao OAuth2 redirect URI |
| `JWT_SECRET` | JWT signing key |
| `HEADER` | JWT header name |
| `PREFIX` | JWT token prefix (e.g. Bearer) |
| `ACCESS_EXP` | JWT access token expiration |
| `GEMINI_API_KEY` | Gemini API key |

## Architecture

**Stack:** Kotlin + Spring Boot 3.5, JPA/Hibernate, MySQL, Spring Security + OAuth2, JWT, OpenFeign

**Package root:** `com.example.zipzabe`

The project follows a layered architecture. The `global/` package holds cross-cutting concerns:

- `global/error/exception/` — Custom exception framework
  - `ErrorCode` — Enum of all error codes, each with an `HttpStatus` and message
  - `ZipzaException` — Base exception class wrapping an `ErrorCode`
  - `ErrorResponse` — Standardized error response body

**Key conventions:**
- Jackson uses `SNAKE_CASE` property naming for all JSON serialization
- JPA is set to `ddl-auto: validate` — schema must be managed externally (migrations), not auto-generated
- Authentication flow: Kakao OAuth2 → JWT access tokens
- OpenFeign is available for external HTTP calls