-- P3-5: Role-based prompt templates
CREATE TABLE prompt_templates (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name             VARCHAR(200) NOT NULL UNIQUE,
    description      VARCHAR(1000),
    system_prompt    TEXT NOT NULL,
    role_type        VARCHAR(50),
    team_id          UUID REFERENCES teams(id) ON DELETE CASCADE,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(255),
    last_modified_by VARCHAR(255),
    version          BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_pt_role   ON prompt_templates(role_type);
CREATE INDEX idx_pt_team   ON prompt_templates(team_id);
CREATE INDEX idx_pt_active ON prompt_templates(active);

-- Default system-wide template
INSERT INTO prompt_templates (id, name, description, system_prompt, active)
VALUES (
    uuid_generate_v4(),
    'Default',
    'Default enterprise knowledge assistant prompt',
    'You are a helpful enterprise knowledge assistant. Answer the user''s question
using ONLY the provided context documents. Follow these rules:

1. Cite sources using [Doc: <document title>] inline when you use information from them.
2. If the context does not contain enough information to answer, say:
   "I don''t have enough information in the available documents to answer that."
3. Do not hallucinate or invent facts not present in the context.
4. Be concise and direct.',
    TRUE
);
