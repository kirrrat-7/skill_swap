# SkillSwap — Learn Together. Grow Together.

SkillSwap is a server-rendered peer-to-peer learning platform for students. It helps students exchange knowledge without money and recommends partners whose teaching and learning goals overlap.

> **Build status:** Phases 1–2 are in progress. The Java 21 / Spring Boot foundation, MySQL/Flyway schema and catalog, database-backed public homepage/search, registration, BCrypt login, hashed email verification, a database-backed overview dashboard, and MySQL integration tests are implemented. Password recovery, rate limiting, social sign-in, MFA, profile/skill editing, matching, exchanges, wallet accounting, notifications, administration, and production hardening remain. This is not production-ready; do not use it for real accounts or sensitive data.

## Stack

- Java 21, Spring Boot 3.5.13, Spring MVC, Spring Security
- Maven, Spring Data JPA/Hibernate, JDBC, Flyway
- MySQL 8.4+ (MySQL 8.4 is used in CI), Thymeleaf, HTML, CSS
- Lucide icons and a locally hosted Inter font; no frontend framework or application JavaScript

The app is a Java application. Maven runs the frontend build-free: templates, styles, icon sprites, and fonts are packaged as static/classpath resources.

## What works in the current build

- Responsive public homepage and searchable MySQL catalog, with database-derived community metrics and honest empty states. Testimonials come only from verified completed-session reviews; there are no fabricated people, reviews, or activity.
- Account registration (name, email, password, college, department, semester, terms), BCrypt password hashing, duplicate-email protection, email verification tokens stored as SHA-256 hashes, and one-time email verification.
- Email/password login is restricted to active, email-verified accounts; CSRF protection stays enabled and sign-out is a CSRF-protected POST.
- A database-backed student overview after login, showing the current account's actual skill, session, credit, hours, badge, and notification totals. Unimplemented dashboard modules are marked as being built rather than linked to dead routes.
- Dev-profile email outbox at `/dev/outbox` (in-memory only) when SMTP is not configured. It is not registered in production.
- Two append-only Flyway migrations and the schema constraints/indexes. The skill catalog seed contains reference taxonomy only, not users or reviews.
- Pages for About, Contact, Privacy, Terms, registration, login, verification, and the local mail preview. The privacy/terms pages are explicitly early drafts needing operator/legal review.
- MySQL 8-backed integration tests cover database migrations and page rendering, skill search/filter, protected dashboard access, registration validation/duplicates, hashed credentials/tokens, email verification, and login.
- HTTP health endpoint at `/actuator/health`.

## Local development

Requirements: JDK 21, Maven 3.9+, and MySQL 8.4+.

1. Create a database and a least-privilege application user. The app runs Flyway on startup:

   ```sql
   CREATE DATABASE skillswap CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   CREATE USER 'skillswap'@'localhost' IDENTIFIED BY 'use-a-unique-local-password';
   GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON skillswap.* TO 'skillswap'@'localhost';
   ```

   Keep schema-migration privileges separate from the runtime account in production.

2. Copy `.env.example` to a private environment file (never commit it), fill in database settings, and export those variables in your shell. For a local HTTP setup set `SKILLSWAP_COOKIE_SECURE=false`.

3. Run:

   ```sh
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

   Open `http://localhost:8080`. Flyway applies the migrations at startup. For a production-style build, use `mvn clean verify` then `java -jar target/skillswap.jar`.

4. Test with a **separate empty** MySQL database. The test profile defaults to `skillswap_test` and reads `SKILLSWAP_TEST_DB_URL`, `SKILLSWAP_TEST_DB_USERNAME`, and `SKILLSWAP_TEST_DB_PASSWORD`.

   ```sh
   export SKILLSWAP_TEST_DB_URL='jdbc:mysql://localhost:3306/skillswap_test'
   export SKILLSWAP_TEST_DB_USERNAME=skillswap
   export SKILLSWAP_TEST_DB_PASSWORD='your-local-test-password'
   mvn test
   ```

   Tests apply Flyway migrations to the test database. **Never point these variables at data you need to keep.**

## Configuration

See `.env.example` for the environment variable names. The production profile is activated with `SPRING_PROFILES_ACTIVE=prod`. Do not put credentials, encryption keys, signing keys, OAuth secrets, or database passwords in YAML, templates, source control, or browser code. Configure secrets in your host's secret manager.

OAuth, outgoing email, production secret checks, backup/restore, deployment verification, and a public HTTPS deployment are not yet implemented. No public URL is available or claimed.

## Data and security notes

- MySQL is the source of truth. The catalog seed contains only reference categories and skill names; user content is read from MySQL.
- All timestamps are stored in UTC; availability is interpreted using the student's saved time zone.
- Skill Credits are an in-app reward only. They are not money, not a cryptocurrency, and cannot be sold or withdrawn.
- The schema stores password hashes only, stores hashed email/reset tokens and recovery codes, and reserves encrypted columns for TOTP secrets. The service implementations are pending.
- Schema changes are append-only Flyway migrations. Do not edit a migration after it has been applied outside a disposable local database; add a new version instead.

## Documentation

- [Architecture and complete route/page inventory](docs/ARCHITECTURE.md)
- [Environment template](.env.example)
- [CI workflow](.github/workflows/ci.yml)

## Current limitations

This is an actively built development version, not a finished learning platform. Do not create real accounts or use sensitive data. Password recovery, resend/expiry recovery for verification emails, social sign-in and account linking, TOTP/recovery codes, profile photo/privacy editing, skill CRUD, recommendation scoring, requests and sessions, atomic wallet rules, review/report workflows, notifications/reminders, admin operations, rate limiting, end-to-end browser accessibility/security checks, and production deployment remain to be implemented and verified. Email delivery requires real SMTP outside the dev/test outbox. The required OAuth provider apps/credentials and a public HTTPS host have not been supplied. Continue in the phased plan in `docs/ARCHITECTURE.md`.

## Credits and licenses

Lucide icons are distributed under the ISC license (see `src/main/resources/static/icons/LUCIDE-LICENSE.txt`). The Inter font is from the `@fontsource-variable/inter` npm package and is distributed under the SIL Open Font License; its license is included at `src/main/resources/static/images/INTER-LICENSE.txt`.
