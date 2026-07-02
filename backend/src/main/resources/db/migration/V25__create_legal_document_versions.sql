CREATE TABLE IF NOT EXISTS legal_document_versions (
    id BIGSERIAL PRIMARY KEY,
    document_type VARCHAR(20) NOT NULL,
    version VARCHAR(20) NOT NULL,
    content_snapshot TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_legal_document_version UNIQUE (document_type, version)
);
