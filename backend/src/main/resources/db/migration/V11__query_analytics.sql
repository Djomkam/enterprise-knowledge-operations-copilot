-- P3-2: Query-level analytics
CREATE TABLE query_analytics (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id          UUID REFERENCES users(id) ON DELETE SET NULL,
    team_id          UUID REFERENCES teams(id) ON DELETE SET NULL,
    conversation_id  UUID REFERENCES conversations(id) ON DELETE SET NULL,
    query            TEXT NOT NULL,
    response_length  INTEGER,
    retrieval_hits   INTEGER DEFAULT 0,
    latency_ms       BIGINT,
    tokens_used      INTEGER,
    model_used       VARCHAR(100),
    success          BOOLEAN NOT NULL DEFAULT TRUE,
    error_message    TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_qa_user    ON query_analytics(user_id);
CREATE INDEX idx_qa_team    ON query_analytics(team_id);
CREATE INDEX idx_qa_created ON query_analytics(created_at);
