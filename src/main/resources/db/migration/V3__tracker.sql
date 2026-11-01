CREATE TABLE applications (
  id              BIGSERIAL PRIMARY KEY,
  user_id         BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  company         VARCHAR(160)  NOT NULL,
  title           VARCHAR(200)  NOT NULL,
  location        VARCHAR(160),
  job_url         VARCHAR(1000),
  source          VARCHAR(40)   NOT NULL DEFAULT 'MANUAL',
  external_job_id VARCHAR(100),
  match_score     INT,
  stage           VARCHAR(20)   NOT NULL,
  position        DOUBLE PRECISION NOT NULL,
  notes           TEXT,
  applied_at      TIMESTAMPTZ,
  follow_up_at    TIMESTAMPTZ,
  follow_up_sent  BOOLEAN       NOT NULL DEFAULT false,
  created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_applications_user_ext ON applications(user_id, external_job_id) WHERE external_job_id IS NOT NULL;
CREATE INDEX idx_applications_board ON applications(user_id, stage, position);
CREATE INDEX idx_applications_follow_up ON applications(follow_up_at) WHERE follow_up_sent = false;

-- stage history, feeds the funnel analytics
CREATE TABLE application_events (
  id             BIGSERIAL PRIMARY KEY,
  application_id BIGINT      NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
  user_id        BIGINT      NOT NULL,
  from_stage     VARCHAR(20),
  to_stage       VARCHAR(20) NOT NULL,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_application_events_user ON application_events(user_id, created_at);
