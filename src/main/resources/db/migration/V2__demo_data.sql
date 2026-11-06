-- Demo data: a fictional hospital site. All people are invented.
-- Every demo account uses the password: demo1234

INSERT INTO sites (id, name) VALUES (1, 'Demo Hospital, Milan');

INSERT INTO areas (id, site_id, code, name) VALUES
    (1, 1, 'WARDS',     'Hospital wards'),
    (2, 1, 'STAIRS',    'Stairwells'),
    (3, 1, 'KITCHEN',   'Kitchen'),
    (4, 1, 'DISHWASH',  'Kitchen wash: food trolleys and dishes'),
    (5, 1, 'CORRIDORS', 'Corridors'),
    (6, 1, 'OFFICES',   'Offices (end of day)');

INSERT INTO shift_templates (id, code, name) VALUES
    (1, 'SUPERVISOR', 'Supervisor day 06:30-12:00 + 13:00-17:00'),
    (2, 'MORNING',    'Morning 08:00-12:00'),
    (3, 'AFTERNOON',  'Afternoon 13:00-17:00'),
    (4, 'SPLIT',      'Split 13:00-15:30 + 17:30-21:30'),
    (5, 'EVENING',    'Evening 17:30-21:30');

INSERT INTO shift_segments (shift_template_id, seq, start_time, end_time) VALUES
    (1, 0, '06:30', '12:00'), (1, 1, '13:00', '17:00'),
    (2, 0, '08:00', '12:00'),
    (3, 0, '13:00', '17:00'),
    (4, 0, '13:00', '15:30'), (4, 1, '17:30', '21:30'),
    (5, 0, '17:30', '21:30');

INSERT INTO coverage_requirements (area_id, shift_template_id, required_headcount) VALUES
    (1, 2, 4),  -- wards, morning
    (1, 3, 2),  -- wards, afternoon
    (2, 2, 1),  -- stairs, morning
    (3, 2, 2),  -- kitchen, morning
    (3, 4, 1),  -- kitchen, split
    (4, 3, 2),  -- dishwash, afternoon
    (5, 2, 2),  -- corridors, morning
    (6, 5, 2);  -- offices, evening

INSERT INTO employees (id, full_name, email, password_hash, role, contract_hours_per_week) VALUES
    (1,  'Admin Demo',        'admin@shiftwise.dev',     '$2a$10$0HW3cusXN1SkUEOexlPVv.NZ8rnpCLZ4ZYaWUsh6ZTO.mkjOOmmPi', 'ADMIN',      40),
    (2,  'Giulia Ferri',      'giulia.ferri@demo.dev',   '$2a$10$0HW3cusXN1SkUEOexlPVv.NZ8rnpCLZ4ZYaWUsh6ZTO.mkjOOmmPi', 'SUPERVISOR', 40),
    (3,  'Marco Bellini',     'marco.bellini@demo.dev',  '$2a$10$0HW3cusXN1SkUEOexlPVv.NZ8rnpCLZ4ZYaWUsh6ZTO.mkjOOmmPi', 'SUPERVISOR', 40),
    (4,  'Sara Conti',        'sara.conti@demo.dev',     '$2a$10$0HW3cusXN1SkUEOexlPVv.NZ8rnpCLZ4ZYaWUsh6ZTO.mkjOOmmPi', 'EMPLOYEE',   20),
    (5,  'Luca Romano',       'luca.romano@demo.dev',    '$2a$10$0HW3cusXN1SkUEOexlPVv.NZ8rnpCLZ4ZYaWUsh6ZTO.mkjOOmmPi', 'EMPLOYEE',   20),
    (6,  'Fatima Benali',     'fatima.benali@demo.dev',  '$2a$10$0HW3cusXN1SkUEOexlPVv.NZ8rnpCLZ4ZYaWUsh6ZTO.mkjOOmmPi', 'EMPLOYEE',   24),
    (7,  'Paolo Greco',       'paolo.greco@demo.dev',    '$2a$10$0HW3cusXN1SkUEOexlPVv.NZ8rnpCLZ4ZYaWUsh6ZTO.mkjOOmmPi', 'EMPLOYEE',   20),
    (8,  'Elena Marino',      'elena.marino@demo.dev',   '$2a$10$0HW3cusXN1SkUEOexlPVv.NZ8rnpCLZ4ZYaWUsh6ZTO.mkjOOmmPi', 'EMPLOYEE',   30),
    (9,  'Youssef Haddad',    'youssef.haddad@demo.dev', '$2a$10$0HW3cusXN1SkUEOexlPVv.NZ8rnpCLZ4ZYaWUsh6ZTO.mkjOOmmPi', 'EMPLOYEE',   20),
    (10, 'Chiara Rizzo',      'chiara.rizzo@demo.dev',   '$2a$10$0HW3cusXN1SkUEOexlPVv.NZ8rnpCLZ4ZYaWUsh6ZTO.mkjOOmmPi', 'EMPLOYEE',   20),
    (11, 'Andrea Gallo',      'andrea.gallo@demo.dev',   '$2a$10$0HW3cusXN1SkUEOexlPVv.NZ8rnpCLZ4ZYaWUsh6ZTO.mkjOOmmPi', 'EMPLOYEE',   30),
    (12, 'Maria Costa',       'maria.costa@demo.dev',    '$2a$10$0HW3cusXN1SkUEOexlPVv.NZ8rnpCLZ4ZYaWUsh6ZTO.mkjOOmmPi', 'EMPLOYEE',   20),
    (13, 'Ion Popescu',       'ion.popescu@demo.dev',    '$2a$10$0HW3cusXN1SkUEOexlPVv.NZ8rnpCLZ4ZYaWUsh6ZTO.mkjOOmmPi', 'EMPLOYEE',   20),
    (14, 'Laura Fontana',     'laura.fontana@demo.dev',  '$2a$10$0HW3cusXN1SkUEOexlPVv.NZ8rnpCLZ4ZYaWUsh6ZTO.mkjOOmmPi', 'EMPLOYEE',   24),
    (15, 'Davide Moretti',    'davide.moretti@demo.dev', '$2a$10$0HW3cusXN1SkUEOexlPVv.NZ8rnpCLZ4ZYaWUsh6ZTO.mkjOOmmPi', 'EMPLOYEE',   20),
    (16, 'Amina Diallo',      'amina.diallo@demo.dev',   '$2a$10$0HW3cusXN1SkUEOexlPVv.NZ8rnpCLZ4ZYaWUsh6ZTO.mkjOOmmPi', 'EMPLOYEE',   20);

-- Skills: who can work where (1 wards, 2 stairs, 3 kitchen, 4 dishwash, 5 corridors, 6 offices)
INSERT INTO employee_areas (employee_id, area_id) VALUES
    (4, 1), (4, 5),
    (5, 1), (5, 2),
    (6, 1), (6, 6),
    (7, 2), (7, 5),
    (8, 3), (8, 4),
    (9, 3), (9, 4),
    (10, 1), (10, 6),
    (11, 3),
    (12, 4), (12, 5),
    (13, 1), (13, 2), (13, 5),
    (14, 1),
    (15, 4), (15, 6),
    (16, 5), (16, 6);

-- Keep sequences ahead of the explicit ids above
SELECT setval('sites_id_seq',           (SELECT MAX(id) FROM sites));
SELECT setval('areas_id_seq',           (SELECT MAX(id) FROM areas));
SELECT setval('shift_templates_id_seq', (SELECT MAX(id) FROM shift_templates));
SELECT setval('employees_id_seq',       (SELECT MAX(id) FROM employees));
