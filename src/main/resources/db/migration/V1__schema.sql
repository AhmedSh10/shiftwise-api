CREATE TABLE sites (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL
);

CREATE TABLE areas (
    id      BIGSERIAL PRIMARY KEY,
    site_id BIGINT       NOT NULL REFERENCES sites (id),
    code    VARCHAR(40)  NOT NULL UNIQUE,
    name    VARCHAR(120) NOT NULL
);

CREATE TABLE employees (
    id                      BIGSERIAL PRIMARY KEY,
    full_name               VARCHAR(120) NOT NULL,
    email                   VARCHAR(160) NOT NULL UNIQUE,
    password_hash           VARCHAR(100) NOT NULL,
    role                    VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'SUPERVISOR', 'EMPLOYEE')),
    contract_hours_per_week INT          NOT NULL CHECK (contract_hours_per_week BETWEEN 1 AND 48),
    active                  BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Which areas an employee is trained to work in
CREATE TABLE employee_areas (
    employee_id BIGINT NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    area_id     BIGINT NOT NULL REFERENCES areas (id) ON DELETE CASCADE,
    PRIMARY KEY (employee_id, area_id)
);

CREATE TABLE shift_templates (
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(40)  NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL
);

-- A shift is one or more time segments, e.g. a split shift 13:00-15:30 + 17:30-21:30
CREATE TABLE shift_segments (
    shift_template_id BIGINT NOT NULL REFERENCES shift_templates (id) ON DELETE CASCADE,
    seq               INT    NOT NULL,
    start_time        TIME   NOT NULL,
    end_time          TIME   NOT NULL CHECK (end_time > start_time),
    PRIMARY KEY (shift_template_id, seq)
);

-- How many people an area needs during a given shift
CREATE TABLE coverage_requirements (
    id                 BIGSERIAL PRIMARY KEY,
    area_id            BIGINT NOT NULL REFERENCES areas (id) ON DELETE CASCADE,
    shift_template_id  BIGINT NOT NULL REFERENCES shift_templates (id) ON DELETE CASCADE,
    required_headcount INT    NOT NULL CHECK (required_headcount > 0),
    UNIQUE (area_id, shift_template_id)
);

CREATE TABLE assignments (
    id                BIGSERIAL PRIMARY KEY,
    employee_id       BIGINT      NOT NULL REFERENCES employees (id),
    area_id           BIGINT      NOT NULL REFERENCES areas (id),
    shift_template_id BIGINT      NOT NULL REFERENCES shift_templates (id),
    work_date         DATE        NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (employee_id, work_date, shift_template_id)
);

CREATE INDEX idx_assignments_date ON assignments (work_date);
CREATE INDEX idx_assignments_employee_date ON assignments (employee_id, work_date);
