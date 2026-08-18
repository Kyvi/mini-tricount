CREATE TABLE expense (
    id BIGSERIAL PRIMARY KEY,
    expense_group_id BIGINT NOT NULL REFERENCES expense_group(id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    expense_date DATE NOT NULL,
    paid_by_participant_id BIGINT NOT NULL REFERENCES participant(id),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE expense_share (
    id BIGSERIAL PRIMARY KEY,
    expense_id BIGINT NOT NULL REFERENCES expense(id) ON DELETE CASCADE,
    participant_id BIGINT NOT NULL REFERENCES participant(id),
    share_amount NUMERIC(10,2) NOT NULL,
    CONSTRAINT uq_expense_share_expense_participant UNIQUE (expense_id, participant_id)
);

CREATE INDEX idx_expense_expense_group_id ON expense(expense_group_id);
-- Pas d'index séparé sur expense_share(expense_id) : déjà couvert par l'index
-- backing la contrainte UNIQUE(expense_id, participant_id) (expense_id en tête de la clé composite).
