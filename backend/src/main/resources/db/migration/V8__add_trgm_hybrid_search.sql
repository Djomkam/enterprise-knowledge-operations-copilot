-- V8: Add pg_trgm full-text index on document_chunks.content for BM25-style keyword search.
-- Combined with the existing pgvector similarity search this enables hybrid (dense + sparse) retrieval.

-- Ensure the trigram extension is available (already created by infra/docker/postgres/init.sql,
-- but we add IF NOT EXISTS so local dev without Docker also works).
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- GIN trigram index for fast ILIKE / similarity queries on chunk content
CREATE INDEX IF NOT EXISTS idx_doc_chunks_content_trgm
    ON document_chunks USING GIN (content gin_trgm_ops);

-- GIN trigram index on document title (used for document-level keyword boosting)
CREATE INDEX IF NOT EXISTS idx_documents_title_trgm
    ON documents USING GIN (title gin_trgm_ops);
