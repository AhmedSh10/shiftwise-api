-- Days an employee cannot work (sick leave, holiday, ...)
CREATE TABLE absences (
    id          BIGSERIAL PRIMARY KEY,
    employee_id BIGINT       NOT NULL REFERENCES employees (id),
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL CHECK (end_date >= start_date),
    reason      VARCHAR(200),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_absences_employee ON absences (employee_id, start_date, end_date);

-- An employee asks a colleague to take one of their shifts
CREATE TABLE swap_requests (
    id                 BIGSERIAL PRIMARY KEY,
    assignment_id      BIGINT      NOT NULL REFERENCES assignments (id) ON DELETE CASCADE,
    requester_id       BIGINT      NOT NULL REFERENCES employees (id),
    target_employee_id BIGINT      NOT NULL REFERENCES employees (id),
    status             VARCHAR(30) NOT NULL CHECK (status IN
                           ('PENDING_COLLEAGUE', 'PENDING_SUPERVISOR', 'APPROVED', 'REJECTED', 'CANCELLED')),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at         TIMESTAMPTZ,
    CHECK (requester_id <> target_employee_id)
);

CREATE INDEX idx_swap_requests_status ON swap_requests (status);
