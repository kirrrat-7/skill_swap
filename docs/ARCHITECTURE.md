# SkillSwap architecture, page inventory, and delivery plan

## Purpose and rules

A student can offer one skill and request another; SkillSwap recommends complementary peers and helps the pair schedule a learning exchange. There is no cash payment. Skill Credits are internal, non-transferable rewards.

- Backend and business logic: Java 21 / Spring Boot. HTML is rendered by Thymeleaf; styling is CSS. No browser-side application framework.
- Persistence: MySQL 8.4+ as the source of truth, Flyway for schema evolution, JDBC for catalog/reporting queries and JPA for aggregate CRUD.
- A public page or empty state must not imply that students, reviews, activity, or outcomes exist when the database has none.
- Every protected operation must be authorized server-side. CSRF stays enabled for forms. No social-auth success path is enabled without real provider credentials.
- Secret/configuration values belong in environment variables or a production secret store, never in Git or templates.

## Current package boundaries

```text
com.skillswap
├── config/       HTTP security and application configuration
├── controller/   Thin MVC endpoints and view-model assembly
├── dto/          Validated request objects and read-only projections
├── entity/       JPA entities and explicit lifecycle state
├── repository/   Spring Data repositories
├── service/      Transactional use cases, policy and orchestration
├── matching/     Configurable, explainable recommendation scoring
├── wallet/       Atomic, idempotent Skill Credit ledger operations
├── notification/ DB notification creation, mail and scheduled reminders
├── validation/   Cross-field and domain validation
└── exception/    Safe user errors and centralized MVC handling
```

Implemented currently: `PublicController` + `PublicCatalogService` for homepage/catalog/review read models; `AccountController` + `RegistrationService` for registration and verified email; `DashboardController` + `DashboardService` for the user's own overview; `InformationalController` for the public policy pages; JPA `UserEntity` and verification-token entity/repositories; dev/test-only in-memory email outbox; and `SecurityConfig` with BCrypt, database-backed email/password login, CSRF, and route protection. Avoid moving business logic into controllers as features are added.

## Route / screen inventory

`[live]` means implemented and covered by the current MySQL-backed integration test. Other rows are the planned acceptance surface; do not link to a planned page as if it were done.

| Area | Route / page | Access | State |
|---|---|---|---|
| Public | `/` — landing page, catalog highlights, how it works, Skill Credits explanation, FAQ | public | live |
| Public | `/skills` — skill catalog, name search, category filter | public | live |
| Public | `/about`, `/contact`, `/privacy`, `/terms` | public | live (draft policy copy; support address must be configured) |
| Account | `/register`, `/login`, `/logout` | public / authenticated | live (email/password; sign-out is POST) |
| Account | `/verify-email`, `/verify-email/sent` | signed link / public | live (resend and expiry recovery planned) |
| Account | `/forgot-password`, `/reset-password` | public, signed token | planned |
| Account | `/oauth2/authorization/{provider}`, provider callback | public | planned; official configuration required |
| Account | `/mfa/challenge`, `/settings/security/mfa` | staged session / authenticated | planned |
| Student | `/dashboard` — overview metrics, activity and progress | authenticated | live (empty state; module navigation is visibly marked as building) |
| Student | `/profile`, `/members/{id}` — editable/private and public views | authenticated / public opt-in | planned |
| Student | `/skills/mine`, `/skills/mine/new`, `/skills/mine/{id}/edit` | authenticated | planned |
| Matching | `/matches`, `/members/{id}/request` | authenticated | planned |
| Requests | `/requests`, `/requests/{id}` and accept/reject/cancel actions | participants | planned |
| Sessions | `/sessions`, `/sessions/{id}`, schedule/reschedule/cancel/confirm | participants | planned |
| Wallet | `/wallet`, `/wallet/transactions` | authenticated | planned |
| Progress | `/progress`, `/goals`, `/goals/{id}` | authenticated | planned |
| Community | `/leaderboard`, `/achievements`, `/notifications` | authenticated / visibility settings | planned |
| Settings | `/settings`, `/settings/privacy`, `/settings/security` | authenticated | planned |
| Admin | `/admin`, `/admin/users`, `/admin/skills`, `/admin/sessions`, `/admin/reports`, `/admin/announcements`, `/admin/audit` | ADMIN only, enforced on server | planned |
| Operations | `/actuator/health` | public, safe status only | live |

The dashboard sidebar should contain Overview, My Profile, My Skills, Find Matches, Learning Requests, Learning Sessions, SkillSwap Wallet, Progress Tracker, Leaderboard, Achievements, Notifications, Settings, and Logout. A future controller must return an actual destination or a disabled/hidden state; do not leave dead links or dummy actions.

## Database model

Migration `V1__core_schema.sql` establishes the required relational core plus supporting tables. The twenty named requirements are represented as `users`, `oauth_accounts`, `email_verification_tokens`, `password_reset_tokens`, `user_mfa_settings`, `mfa_recovery_codes`, `skills`, `user_skills`, `student_availability`, `skill_requests`, `sessions`, `reviews`, `notifications`, `wallets`, `wallet_transactions`, `achievements`, `user_achievements`, `learning_goals`, `learning_progress`, and `admin_audit_logs`.

Supporting tables currently include `user_photos`, `remember_me_tokens`, `security_events`, `skill_categories`, `user_interests`, `content_reports`, `goal_milestones`, and `announcements`. They cover profile uploads, remember-me revocation, security-event history, category management, reporting, measurable learning goals, and admin announcements. Foreign keys, unique indexes, query indexes, check constraints, optimistic version fields, and UTC timestamp columns are specified in SQL.

`V2__skill_catalog.sql` inserts only the 10 taxonomy categories and catalog skill names. It does not insert users, public mentors, testimonials, or reviews. The core schema includes:

- token hashes rather than raw verification/reset tokens; encrypted TOTP secret columns and hashed recovery codes;
- unique active request keys (written transactionally by the service), unique reviewer/session pairs, and unique ledger idempotency keys;
- two-person session outcome fields and dispute/resolution fields; wallet reservation/reward values snapshotted onto the session;
- an append-only wallet transaction ledger plus a balance row to lock and update inside a transaction.

**Important:** the service-level invariants are not all implemented yet. Schema constraints alone do not deliver verified-session credits, conflict prevention, MFA replay protection, or profile authorization.

## Matching score contract

The planned matching service uses normalized 0–100 components and configurable weights from `application.yml`:

- skill compatibility: 40%; reciprocal compatibility: 25%; overlapping availability: 15%; proficiency suitability: 10%; learning preferences: 10%.
- Each component must be normalized before weighting. A candidate with no actual teach/learn overlap is not a match and never receives a high score through profile popularity, rating, or completeness.
- Persist/return each component and a concise explanation, not just a number. Rating/institution/location may be shown separately or added only after explicit validation of the weights.
- Weights must sum to 100 at startup or in a tested configuration validator.

## Security boundaries

Planned implementation: BCrypt password hashes, generic account-recovery/login errors, input validation, rate limits, CSRF, HttpOnly/Secure/SameSite session cookies, session fixation protection, account-linking confirmation, TOTP AES-GCM encryption, one-time recovery codes, and server-side authorization at each controller/service boundary. Social providers use Spring Security OAuth/OIDC and store only the stable subject plus provider metadata; they never supply a local password. Matching/profile privacy is enforced in the query and service, not by hiding a template button.

The current `SecurityConfig` provides BCrypt-backed email/password authentication from `users`, requires active and email-verified accounts, enables CSRF, uses Spring Security's session-fixation protection, and protects all routes except the explicit public allowlist. This is not the final account system: recovery, brute-force/rate limits, OAuth, TOTP, session-version revocation, and admin role workflows are still missing. Do not deploy this version as a user service.

## Transactional invariants to test before completion

1. Registration, duplicate-email races, verification-token reuse/expiry, reset-token reuse/expiry, and credential/session revocation.
2. OAuth provider/subject uniqueness; no merge on matching email without the account holder authenticating and confirming.
3. TOTP RFC vectors, ±1 time window, one-step replay rejection, attempt limits/lockout, recovery code single use, reauthentication to disable/regenerate.
4. Skill/request ownership; no self-request, duplicate active request, or unauthorized state change.
5. UTC conversion, double-booking conflicts, reschedule consent, missed/disputed sessions, two-party completion, and verified review uniqueness.
6. Wallet row lock / optimistic locking, idempotency key collisions, no negative spend, reservation refund, and exactly-once teaching award.
7. Role checks on every admin service/controller; opt-outs for public profiles and leaderboards.

## Phased delivery

| Phase | Scope | Completion gate |
|---|---|---|
| 1 — foundation | Maven/Java configuration, schema/migrations, architecture/page inventory, homepage | MySQL migrations start cleanly; responsive home and catalog; integration tests green |
| 2 — account basics | registration, BCrypt login, email verification/reset, profile completion, dashboard shell | registration/login/logout/reset tests; DB-backed session and CSRF checks |
| 3 — security | OAuth/OIDC setup-required states, account linking, TOTP/recovery, rate limits, mail | RFC tests, security negative tests, provider setup documented |
| 4 — profiles and skills | privacy, photo upload, skill CRUD, availability | ownership validation and input tests |
| 5 — matching/requests | documented weighted matching and request lifecycle | reciprocal-match and request-race tests |
| 6 — sessions/community | scheduling, conflicts, outcomes, reviews, notifications/reminders | timezone, conflict, double-completion and verified-review tests |
| 7 — rewards | atomic wallet, progress, milestones, badges, leaderboard | ledger consistency, idempotency, opt-out tests |
| 8 — admin | accounts, category catalogue, reports, announcements, audit and aggregate stats | ADMIN-only server tests for every admin action |
| 9 — integration hardening | complete JUnit/MockMvc/MySQL coverage, browser screen and action walkthrough, accessibility and security checks | clean build/test; all navigation/forms/actions verified; fix and rerun |
| 10 — deployment | production secrets, TLS, MySQL backup/restore, OAuth callbacks, health/logs, release | user supplies host/DB/DNS/OAuth access; deployment and HTTPS URL verified |

Run each phase as PLAN → BUILD → RUN (app + tests) → CHECK (all affected screens/actions) → FIX → RUN again. A green compile is not an end-to-end completion claim.

## Deployment boundary

The checked-in Dockerfile and Compose configuration are for packaging/local practice only; no image has been built or deployed from this restricted sandbox. A production operator must create a managed MySQL database, apply least-privilege migration/runtime grants, provision environment secrets, configure a stable HTTPS origin and proxy headers, add each provider's callback URI, and test migrations/backups/health on the actual host. Hosting account, billing, DNS, SMTP credentials, and OAuth provider credentials must be supplied by the owner; none are inferred here.
