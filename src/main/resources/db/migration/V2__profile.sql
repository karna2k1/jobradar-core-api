CREATE TABLE profiles (
  user_id             BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  headline            VARCHAR(160),
  experience_years    NUMERIC(4,1),
  current_ctc_lpa     NUMERIC(6,2),
  expected_ctc_lpa    NUMERIC(6,2),
  notice_period_days  INT,
  work_mode           VARCHAR(10)  NOT NULL DEFAULT 'ANY',
  skills              JSONB        NOT NULL DEFAULT '[]',
  target_roles        JSONB        NOT NULL DEFAULT '[]',
  locations           JSONB        NOT NULL DEFAULT '[]',
  blacklist_companies JSONB        NOT NULL DEFAULT '[]',
  min_match_score     INT          NOT NULL DEFAULT 70,
  quiet_hours_start   TIME,
  quiet_hours_end     TIME,
  updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE resumes (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  file_name    VARCHAR(255) NOT NULL,
  content_text TEXT         NOT NULL,
  uploaded_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_resumes_user ON resumes(user_id, uploaded_at DESC);
