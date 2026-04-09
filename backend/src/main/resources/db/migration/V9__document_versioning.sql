-- P3-1: Document versioning support
ALTER TABLE documents
    ADD COLUMN parent_id     UUID REFERENCES documents(id) ON DELETE SET NULL,
    ADD COLUMN version_number INTEGER NOT NULL DEFAULT 1;

CREATE INDEX idx_doc_parent      ON documents(parent_id);
CREATE INDEX idx_doc_fn_owner    ON documents(file_name, owner_id);
