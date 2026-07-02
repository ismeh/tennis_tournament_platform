CREATE TABLE consent_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    document_type VARCHAR(20) NOT NULL,
    action VARCHAR(10) NOT NULL,
    document_version_id BIGINT NOT NULL REFERENCES legal_document_versions(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consent_records_user_id ON consent_records(user_id);
CREATE INDEX idx_consent_records_user_doc ON consent_records(user_id, document_type);
