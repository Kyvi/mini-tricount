CREATE TABLE expense_group (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE participant (
    id BIGSERIAL PRIMARY KEY,
    expense_group_id BIGINT NOT NULL REFERENCES expense_group(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL
);

CREATE INDEX idx_participant_expense_group_id ON participant(expense_group_id);
