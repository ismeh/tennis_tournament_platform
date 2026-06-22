CREATE TABLE schedule_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL UNIQUE REFERENCES tournaments(id) ON DELETE CASCADE,
    match_duration_minutes INTEGER NOT NULL DEFAULT 60
);

CREATE TABLE time_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_config_id UUID NOT NULL REFERENCES schedule_configs(id) ON DELETE CASCADE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_time_slots_config ON time_slots (schedule_config_id);
