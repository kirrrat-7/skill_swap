-- SkillSwap core schema (MySQL 8+).
-- All timestamps are stored in UTC as DATETIME(6). Booleans are TINYINT(1).
-- Status-like columns are VARCHAR guarded by CHECK constraints so that allowed values are visible in the schema.

-- ---------------------------------------------------------------------------
-- Accounts
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    email                 VARCHAR(254) NOT NULL,
    password_hash         VARCHAR(100) NULL COMMENT 'BCrypt hash. NULL for accounts that only sign in through a social provider.',
    full_name             VARCHAR(100) NOT NULL,
    college               VARCHAR(150) NULL,
    department            VARCHAR(100) NULL,
    semester              TINYINT      NULL,
    bio                   VARCHAR(500) NULL,
    role                  VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    status                VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    email_verified_at     DATETIME(6)  NULL,
    timezone              VARCHAR(64)  NOT NULL DEFAULT 'UTC',
    profile_public        TINYINT(1)   NOT NULL DEFAULT 0,
    show_college          TINYINT(1)   NOT NULL DEFAULT 1,
    show_reviews          TINYINT(1)   NOT NULL DEFAULT 1,
    leaderboard_visible   TINYINT(1)   NOT NULL DEFAULT 1,
    email_notifications   TINYINT(1)   NOT NULL DEFAULT 1,
    auth_version          INT          NOT NULL DEFAULT 0 COMMENT 'Incremented when credentials or access change; older sessions are signed out.',
    last_login_at         DATETIME(6)  NULL,
    terms_accepted_at     DATETIME(6)  NOT NULL,
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6)  NOT NULL,
    version               BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_status (status),
    KEY idx_users_college (college),
    KEY idx_users_created (created_at),
    CONSTRAINT chk_users_role CHECK (role IN ('STUDENT', 'ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    CONSTRAINT chk_users_semester CHECK (semester IS NULL OR semester BETWEEN 1 AND 12)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE user_photos (
    user_id      BIGINT      NOT NULL,
    content_type VARCHAR(30) NOT NULL,
    data         MEDIUMBLOB  NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_photos_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_photos_type CHECK (content_type IN ('image/jpeg', 'image/png'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE oauth_accounts (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    provider         VARCHAR(20)  NOT NULL,
    provider_user_id VARCHAR(191) NOT NULL COMMENT 'Stable subject identifier issued by the provider. Never a password or token.',
    email            VARCHAR(254) NULL,
    display_name     VARCHAR(150) NULL,
    linked_at        DATETIME(6)  NOT NULL,
    last_login_at    DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_oauth_provider_subject (provider, provider_user_id),
    UNIQUE KEY uk_oauth_user_provider (user_id, provider),
    CONSTRAINT fk_oauth_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_oauth_provider CHECK (provider IN ('GOOGLE', 'APPLE', 'X'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE email_verification_tokens (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    email      VARCHAR(254) NOT NULL,
    token_hash CHAR(64)     NOT NULL COMMENT 'SHA-256 of the token. The token itself is only ever in the e-mail.',
    expires_at DATETIME(6)  NOT NULL,
    used_at    DATETIME(6)  NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_evt_token_hash (token_hash),
    KEY idx_evt_user (user_id),
    KEY idx_evt_expires (expires_at),
    CONSTRAINT fk_evt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE password_reset_tokens (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    token_hash   CHAR(64)    NOT NULL COMMENT 'SHA-256 of the token.',
    expires_at   DATETIME(6) NOT NULL,
    used_at      DATETIME(6) NULL,
    requested_ip VARCHAR(45) NULL,
    created_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_prt_token_hash (token_hash),
    KEY idx_prt_user (user_id),
    KEY idx_prt_expires (expires_at),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE remember_me_tokens (
    series     VARCHAR(64)  NOT NULL,
    email      VARCHAR(254) NOT NULL,
    token_hash CHAR(64)     NOT NULL COMMENT 'SHA-256 of the cookie token.',
    last_used  DATETIME(6)  NOT NULL,
    PRIMARY KEY (series),
    KEY idx_rmt_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE user_mfa_settings (
    user_id                  BIGINT       NOT NULL,
    enabled                  TINYINT(1)   NOT NULL DEFAULT 0,
    secret_encrypted         VARCHAR(255) NULL COMMENT 'TOTP secret, AES-256-GCM encrypted with the application key.',
    pending_secret_encrypted VARCHAR(255) NULL COMMENT 'Secret generated during enrolment, before the first code is confirmed.',
    enabled_at               DATETIME(6)  NULL,
    last_used_step           BIGINT       NULL COMMENT 'Last accepted 30-second time step. Prevents a code being replayed.',
    failed_attempts          INT          NOT NULL DEFAULT 0,
    locked_until             DATETIME(6)  NULL,
    updated_at               DATETIME(6)  NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_mfa_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_mfa_enabled_has_secret CHECK (enabled = 0 OR secret_encrypted IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE mfa_recovery_codes (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    code_hash  VARCHAR(100) NOT NULL COMMENT 'BCrypt hash of a one-time recovery code.',
    used_at    DATETIME(6)  NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_recovery_user (user_id, used_at),
    CONSTRAINT fk_recovery_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE security_events (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NULL,
    email      VARCHAR(254) NULL,
    event_type VARCHAR(40)  NOT NULL,
    ip_address VARCHAR(45)  NULL,
    user_agent VARCHAR(255) NULL,
    details    VARCHAR(255) NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_secevt_user (user_id, created_at),
    KEY idx_secevt_email_type (email, event_type, created_at),
    KEY idx_secevt_created (created_at),
    CONSTRAINT fk_secevt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Skills
-- ---------------------------------------------------------------------------
CREATE TABLE skill_categories (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    name          VARCHAR(60) NOT NULL,
    slug          VARCHAR(60) NOT NULL,
    icon          VARCHAR(40) NOT NULL DEFAULT 'shapes',
    display_order INT         NOT NULL DEFAULT 0,
    active        TINYINT(1)  NOT NULL DEFAULT 1,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_name (name),
    UNIQUE KEY uk_categories_slug (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE skills (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    category_id BIGINT      NOT NULL,
    name        VARCHAR(80) NOT NULL,
    created_by  BIGINT      NULL,
    created_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_skills_name (name),
    KEY idx_skills_category (category_id),
    CONSTRAINT fk_skills_category FOREIGN KEY (category_id) REFERENCES skill_categories (id),
    CONSTRAINT fk_skills_creator FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE user_interests (
    user_id     BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, category_id),
    CONSTRAINT fk_interests_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_interests_category FOREIGN KEY (category_id) REFERENCES skill_categories (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE user_skills (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    skill_id          BIGINT       NOT NULL,
    type              VARCHAR(10)  NOT NULL COMMENT 'TEACH = can teach, LEARN = wants to learn',
    proficiency_level VARCHAR(20)  NOT NULL,
    experience_level  VARCHAR(20)  NOT NULL,
    learning_mode     VARCHAR(20)  NOT NULL,
    description       VARCHAR(500) NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_skills (user_id, skill_id, type),
    KEY idx_user_skills_skill_type (skill_id, type),
    CONSTRAINT fk_user_skills_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_skills_skill FOREIGN KEY (skill_id) REFERENCES skills (id),
    CONSTRAINT chk_user_skills_type CHECK (type IN ('TEACH', 'LEARN')),
    CONSTRAINT chk_user_skills_proficiency CHECK (proficiency_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT')),
    CONSTRAINT chk_user_skills_experience CHECK (experience_level IN ('NONE', 'UNDER_ONE_YEAR', 'ONE_TO_THREE_YEARS', 'THREE_TO_FIVE_YEARS', 'OVER_FIVE_YEARS')),
    CONSTRAINT chk_user_skills_mode CHECK (learning_mode IN ('ONLINE', 'IN_PERSON', 'EITHER'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE student_availability (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    day_of_week TINYINT     NOT NULL COMMENT '1 = Monday ... 7 = Sunday, in the student''s own time zone',
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_availability_slot (user_id, day_of_week, start_time),
    CONSTRAINT fk_availability_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_availability_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_availability_range CHECK (start_time < end_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Exchange requests and sessions
-- ---------------------------------------------------------------------------
CREATE TABLE skill_requests (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    sender_id          BIGINT       NOT NULL,
    receiver_id        BIGINT       NOT NULL,
    requested_skill_id BIGINT       NOT NULL COMMENT 'What the sender wants to learn from the receiver',
    offered_skill_id   BIGINT       NULL COMMENT 'What the sender offers to teach in return',
    message            VARCHAR(500) NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    responded_at       DATETIME(6)  NULL,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0,
    -- Populated only while the request is PENDING or ACCEPTED. The unique key is
    -- calculated and set by the request service in the same transaction as the status.
    active_key         VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_requests_active (active_key),
    KEY idx_requests_receiver (receiver_id, status, created_at),
    KEY idx_requests_sender (sender_id, status, created_at),
    CONSTRAINT fk_requests_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_requests_receiver FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_requests_requested_skill FOREIGN KEY (requested_skill_id) REFERENCES skills (id),
    CONSTRAINT fk_requests_offered_skill FOREIGN KEY (offered_skill_id) REFERENCES skills (id),
    CONSTRAINT chk_requests_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_requests_not_self CHECK (sender_id <> receiver_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE sessions (
    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    request_id                BIGINT        NOT NULL,
    teacher_id                BIGINT        NOT NULL,
    learner_id                BIGINT        NOT NULL,
    skill_id                  BIGINT        NOT NULL,
    topic                     VARCHAR(150)  NOT NULL,
    description               VARCHAR(1000) NULL,
    mode                      VARCHAR(20)   NOT NULL,
    meeting_link              VARCHAR(500)  NULL,
    location                  VARCHAR(200)  NULL,
    starts_at                 DATETIME(6)   NOT NULL,
    ends_at                   DATETIME(6)   NOT NULL,
    duration_minutes          SMALLINT      NOT NULL,
    status                    VARCHAR(20)   NOT NULL DEFAULT 'SCHEDULED',
    scheduled_by              BIGINT        NOT NULL,
    teacher_outcome           VARCHAR(12)   NULL COMMENT 'What the teacher reported after the session: COMPLETED or MISSED',
    teacher_outcome_at        DATETIME(6)   NULL,
    learner_outcome           VARCHAR(12)   NULL,
    learner_outcome_at        DATETIME(6)   NULL,
    disputed                  TINYINT(1)    NOT NULL DEFAULT 0,
    dispute_note              VARCHAR(300)  NULL,
    resolved_by               BIGINT        NULL,
    resolved_at               DATETIME(6)   NULL,
    proposed_starts_at        DATETIME(6)   NULL COMMENT 'Reschedule proposal awaiting the other participant',
    proposed_duration_minutes SMALLINT      NULL,
    proposed_by               BIGINT        NULL,
    proposed_at               DATETIME(6)   NULL,
    cancelled_by              BIGINT        NULL,
    cancel_reason             VARCHAR(300)  NULL,
    cancelled_at              DATETIME(6)   NULL,
    completed_at              DATETIME(6)   NULL,
    reminder_sent_at          DATETIME(6)   NULL,
    credits_cost              INT           NOT NULL COMMENT 'Skill Credits reserved from the learner when the session was booked',
    credits_reward            INT           NOT NULL COMMENT 'Skill Credits the teacher earns when completion is verified',
    created_at                DATETIME(6)   NOT NULL,
    updated_at                DATETIME(6)   NOT NULL,
    version                   BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_sessions_teacher (teacher_id, starts_at),
    KEY idx_sessions_learner (learner_id, starts_at),
    KEY idx_sessions_status (status, starts_at),
    KEY idx_sessions_request (request_id),
    KEY idx_sessions_completed (completed_at),
    CONSTRAINT fk_sessions_request FOREIGN KEY (request_id) REFERENCES skill_requests (id),
    CONSTRAINT fk_sessions_teacher FOREIGN KEY (teacher_id) REFERENCES users (id),
    CONSTRAINT fk_sessions_learner FOREIGN KEY (learner_id) REFERENCES users (id),
    CONSTRAINT fk_sessions_skill FOREIGN KEY (skill_id) REFERENCES skills (id),
    CONSTRAINT fk_sessions_scheduler FOREIGN KEY (scheduled_by) REFERENCES users (id),
    CONSTRAINT fk_sessions_resolver FOREIGN KEY (resolved_by) REFERENCES users (id),
    CONSTRAINT fk_sessions_proposer FOREIGN KEY (proposed_by) REFERENCES users (id),
    CONSTRAINT fk_sessions_canceller FOREIGN KEY (cancelled_by) REFERENCES users (id),
    CONSTRAINT chk_sessions_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED', 'MISSED')),
    CONSTRAINT chk_sessions_mode CHECK (mode IN ('ONLINE', 'OFFLINE')),
    CONSTRAINT chk_sessions_participants CHECK (teacher_id <> learner_id),
    CONSTRAINT chk_sessions_duration CHECK (duration_minutes BETWEEN 15 AND 480),
    CONSTRAINT chk_sessions_range CHECK (starts_at < ends_at),
    CONSTRAINT chk_sessions_teacher_outcome CHECK (teacher_outcome IS NULL OR teacher_outcome IN ('COMPLETED', 'MISSED')),
    CONSTRAINT chk_sessions_learner_outcome CHECK (learner_outcome IS NULL OR learner_outcome IN ('COMPLETED', 'MISSED')),
    CONSTRAINT chk_sessions_credits CHECK (credits_cost >= 0 AND credits_reward >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE reviews (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    session_id  BIGINT        NOT NULL,
    reviewer_id BIGINT        NOT NULL,
    reviewee_id BIGINT        NOT NULL,
    rating      TINYINT       NOT NULL,
    comment     VARCHAR(1000) NULL,
    hidden      TINYINT(1)    NOT NULL DEFAULT 0 COMMENT 'Set by an administrator after a report is upheld',
    created_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reviews_session_reviewer (session_id, reviewer_id),
    KEY idx_reviews_reviewee (reviewee_id, hidden, created_at),
    CONSTRAINT fk_reviews_session FOREIGN KEY (session_id) REFERENCES sessions (id),
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_reviewee FOREIGN KEY (reviewee_id) REFERENCES users (id),
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_reviews_not_self CHECK (reviewer_id <> reviewee_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE content_reports (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    review_id       BIGINT       NOT NULL,
    reporter_id     BIGINT       NOT NULL,
    reason          VARCHAR(30)  NOT NULL,
    details         VARCHAR(500) NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    resolved_by     BIGINT       NULL,
    resolved_at     DATETIME(6)  NULL,
    resolution_note VARCHAR(300) NULL,
    created_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reports_review_reporter (review_id, reporter_id),
    KEY idx_reports_status (status, created_at),
    CONSTRAINT fk_reports_review FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
    CONSTRAINT fk_reports_resolver FOREIGN KEY (resolved_by) REFERENCES users (id),
    CONSTRAINT chk_reports_status CHECK (status IN ('OPEN', 'ACTIONED', 'DISMISSED')),
    CONSTRAINT chk_reports_reason CHECK (reason IN ('OFFENSIVE', 'HARASSMENT', 'SPAM', 'FALSE_INFORMATION', 'OTHER'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE notifications (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    type       VARCHAR(40)  NOT NULL,
    title      VARCHAR(150) NOT NULL,
    message    VARCHAR(500) NOT NULL,
    link       VARCHAR(255) NULL,
    read_at    DATETIME(6)  NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notifications_user_unread (user_id, read_at),
    KEY idx_notifications_user_created (user_id, created_at),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Wallet (in-app Skill Credits only: not money, not transferable out of the platform)
-- ---------------------------------------------------------------------------
CREATE TABLE wallets (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    balance      INT         NOT NULL DEFAULT 0,
    total_earned INT         NOT NULL DEFAULT 0,
    total_spent  INT         NOT NULL DEFAULT 0,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallets_user (user_id),
    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_wallets_balance CHECK (balance >= 0),
    CONSTRAINT chk_wallets_totals CHECK (total_earned >= 0 AND total_spent >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE achievements (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    code               VARCHAR(50)  NOT NULL,
    name               VARCHAR(80)  NOT NULL,
    description        VARCHAR(255) NOT NULL,
    icon               VARCHAR(40)  NOT NULL DEFAULT 'award',
    points             INT          NOT NULL DEFAULT 0,
    bonus_credits      INT          NOT NULL DEFAULT 0,
    criteria_type      VARCHAR(40)  NOT NULL,
    criteria_threshold INT          NOT NULL,
    criteria_skill     VARCHAR(80)  NULL,
    display_order      INT          NOT NULL DEFAULT 0,
    active             TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_achievements_code (code),
    CONSTRAINT chk_achievements_values CHECK (points >= 0 AND bonus_credits >= 0 AND criteria_threshold > 0),
    CONSTRAINT chk_achievements_criteria CHECK (criteria_type IN
        ('SESSIONS_COMPLETED', 'SESSIONS_TAUGHT', 'SESSIONS_LEARNED', 'DISTINCT_LEARNERS_TAUGHT', 'SKILL_SESSIONS_TAUGHT', 'LEARNING_STREAK_WEEKS'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE user_achievements (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    user_id        BIGINT      NOT NULL,
    achievement_id BIGINT      NOT NULL,
    earned_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_achievements (user_id, achievement_id),
    KEY idx_user_achievements_earned (earned_at),
    CONSTRAINT fk_user_achievements_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_achievements_achievement FOREIGN KEY (achievement_id) REFERENCES achievements (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE wallet_transactions (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    wallet_id       BIGINT       NOT NULL,
    type            VARCHAR(30)  NOT NULL,
    amount          INT          NOT NULL COMMENT 'Positive = credit, negative = debit',
    balance_after   INT          NOT NULL,
    description     VARCHAR(255) NOT NULL,
    session_id      BIGINT       NULL,
    achievement_id  BIGINT       NULL,
    idempotency_key VARCHAR(120) NOT NULL COMMENT 'One row per business event; a repeated operation hits the unique index instead of paying twice.',
    created_by      BIGINT       NULL,
    created_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_tx_idempotency (idempotency_key),
    KEY idx_wallet_tx_wallet (wallet_id, created_at),
    KEY idx_wallet_tx_session (session_id),
    KEY idx_wallet_tx_created (created_at),
    CONSTRAINT fk_wallet_tx_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id) ON DELETE CASCADE,
    CONSTRAINT fk_wallet_tx_session FOREIGN KEY (session_id) REFERENCES sessions (id),
    CONSTRAINT fk_wallet_tx_achievement FOREIGN KEY (achievement_id) REFERENCES achievements (id),
    CONSTRAINT fk_wallet_tx_creator FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_wallet_tx_amount CHECK (amount <> 0),
    CONSTRAINT chk_wallet_tx_balance CHECK (balance_after >= 0),
    CONSTRAINT chk_wallet_tx_type CHECK (type IN
        ('SIGNUP_BONUS', 'SESSION_RESERVED', 'SESSION_REFUND', 'TEACHING_REWARD', 'ACHIEVEMENT_BONUS', 'ADMIN_ADJUSTMENT'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Learning goals and progress
-- ---------------------------------------------------------------------------
CREATE TABLE learning_goals (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    skill_id     BIGINT       NOT NULL,
    title        VARCHAR(150) NOT NULL,
    target_date  DATE         NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    completed_at DATETIME(6)  NULL,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_goals_user (user_id, status),
    CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_goals_skill FOREIGN KEY (skill_id) REFERENCES skills (id),
    CONSTRAINT chk_goals_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'ARCHIVED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- A goal's progress percentage is completed milestones divided by total milestones.
CREATE TABLE goal_milestones (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    goal_id      BIGINT       NOT NULL,
    title        VARCHAR(150) NOT NULL,
    position     INT          NOT NULL,
    completed_at DATETIME(6)  NULL,
    created_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_milestones_position (goal_id, position),
    CONSTRAINT fk_milestones_goal FOREIGN KEY (goal_id) REFERENCES learning_goals (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE learning_progress (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    user_id            BIGINT      NOT NULL,
    skill_id           BIGINT      NOT NULL,
    sessions_completed INT         NOT NULL DEFAULT 0,
    minutes_learned    INT         NOT NULL DEFAULT 0,
    last_session_at    DATETIME(6) NULL,
    updated_at         DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_progress_user_skill (user_id, skill_id),
    CONSTRAINT fk_progress_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_progress_skill FOREIGN KEY (skill_id) REFERENCES skills (id),
    CONSTRAINT chk_progress_values CHECK (sessions_completed >= 0 AND minutes_learned >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Administration
-- ---------------------------------------------------------------------------
CREATE TABLE announcements (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    title      VARCHAR(150)  NOT NULL,
    body       VARCHAR(1000) NOT NULL,
    published  TINYINT(1)    NOT NULL DEFAULT 0,
    starts_at  DATETIME(6)   NULL,
    ends_at    DATETIME(6)   NULL,
    created_by BIGINT        NULL,
    created_at DATETIME(6)   NOT NULL,
    updated_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_announcements_published (published, starts_at, ends_at),
    CONSTRAINT fk_announcements_creator FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE admin_audit_logs (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    admin_id    BIGINT        NULL,
    admin_email VARCHAR(254)  NOT NULL COMMENT 'Kept so the entry still identifies the actor if the account is later removed',
    action      VARCHAR(60)   NOT NULL,
    target_type VARCHAR(40)   NULL,
    target_id   BIGINT        NULL,
    details     VARCHAR(1000) NULL,
    ip_address  VARCHAR(45)   NULL,
    created_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_audit_admin (admin_id, created_at),
    KEY idx_audit_target (target_type, target_id),
    KEY idx_audit_created (created_at),
    CONSTRAINT fk_audit_admin FOREIGN KEY (admin_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
