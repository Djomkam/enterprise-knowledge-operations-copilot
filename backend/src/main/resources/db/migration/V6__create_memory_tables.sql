-- Create memory_entries table with vector embeddings
CREATE TABLE memory_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    memory_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    metadata TEXT,
    embedding vector(1536),
    relevance_score DOUBLE PRECISION,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes
CREATE INDEX idx_memory_user ON memory_entries(user_id);
CREATE INDEX idx_memory_type ON memory_entries(memory_type);

-- Create vector similarity index for memory retrieval
CREATE INDEX ON memory_entries USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
