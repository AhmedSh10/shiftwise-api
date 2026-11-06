# Shiftwise API

[![CI](https://github.com/AhmedSh10/shiftwise-api/actions/workflows/ci.yml/badge.svg)](https://github.com/AhmedSh10/shiftwise-api/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot 3](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)

Shift scheduling for facility-services teams: the cleaning crews that keep a hospital running.

Inspired by real work on a hospital cleaning contract in Milan. All data in this repository is invented.

## The problem

On a hospital site the schedule is still built by hand by supervisors, and it breaks in three ways:

1. **Someone is absent** and the gap is only noticed later. Gaps pile up and one area ends up short while another is overstaffed.
2. **People are placed where they are not trained.** Someone who knows the kitchen wash is sent to the wards.
3. **Shifts are irregular.** Besides 4-hour morning and afternoon shifts there are split shifts (13:00-15:30 + 17:30-21:30) and long supervisor days, so overlaps and rest periods are easy to get wrong.

## What the API does

| | |
|---|---|
| **Areas and headcount** | Each area (wards, stairs, kitchen, kitchen wash, corridors, offices) declares how many people it needs per shift |
| **Skills** | Each employee lists the areas they can work in. Assigning someone elsewhere is rejected |
| **Split shifts** | A shift is a list of time segments, so a split shift counts 6.5 worked hours, not 8.5, and a shift in its gap does not clash |
| **Rule checks** | Every assignment is checked before it is saved. A broken rule returns `422` with the rule name |
| **Coverage board** | For any day: required vs assigned per area and shift, with a GREEN / YELLOW / RED status |
| **Roles** | `ADMIN`, `SUPERVISOR`, `EMPLOYEE`, with JWT authentication |

### Roadmap

- [x] **Week 1:** domain model, JWT and roles, assignments with skill and overlap checks, coverage board, Testcontainers tests, Docker, CI
- [ ] **Week 2:** 11-hour rest rule between shifts, weekly contract hours, absences, shift swap requests
- [ ] **Week 3:** replacement suggestions when someone is absent, weekly schedule generation

## Run it

You need Docker.

```bash
docker compose up --build
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

### Demo accounts (password `demo1234`)

| Role | Email |
|---|---|
| Admin | `admin@shiftwise.dev` |
| Supervisor | `giulia.ferri@demo.dev` |
| Employee | `sara.conti@demo.dev` |

### Try it

```bash
TOKEN=$(curl -s localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"giulia.ferri@demo.dev","password":"demo1234"}' | jq -r .token)

# Put Sara (wards, corridors) on the wards morning shift
curl -s localhost:8080/api/assignments -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"employeeId":4,"areaId":1,"shiftTemplateId":2,"date":"2026-11-09"}'

# Try to send her to the kitchen: 422 NOT_QUALIFIED_FOR_AREA
curl -s localhost:8080/api/assignments -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"employeeId":4,"areaId":3,"shiftTemplateId":2,"date":"2026-11-09"}'

# How covered is that day?
curl -s "localhost:8080/api/coverage?date=2026-11-09" -H "Authorization: Bearer $TOKEN" | jq
```

## API

| Method | Path | Who |
|---|---|---|
| `POST` | `/api/auth/login` | everyone |
| `GET` | `/api/me` | signed in |
| `GET` | `/api/areas` | signed in |
| `POST` | `/api/areas` | admin |
| `GET` | `/api/shift-templates` | signed in |
| `GET` `PUT` | `/api/coverage-requirements` | read: signed in, write: supervisor |
| `GET` | `/api/coverage?date=` | supervisor |
| `GET` | `/api/employees`, `/api/employees/{id}` | supervisor |
| `POST` | `/api/employees` | admin |
| `PUT` | `/api/employees/{id}/skills` | supervisor |
| `PATCH` | `/api/employees/{id}/active?value=` | admin |
| `GET` | `/api/assignments?from=&to=` | signed in |
| `POST` `DELETE` | `/api/assignments` | supervisor |

Errors follow RFC 7807. A rule violation looks like this:

```json
{
  "status": 422,
  "title": "Scheduling rule violated",
  "detail": "Sara Conti is not trained for Kitchen",
  "violations": [{ "rule": "NOT_QUALIFIED_FOR_AREA", "message": "Sara Conti is not trained for Kitchen" }]
}
```

## Design notes

- **Rules are plain Java.** `AssignmentRules` has no Spring or database code, so it is unit-tested directly and will be reused to rank replacement candidates.
- **Split shifts as segments.** `ShiftTemplate` holds an ordered list of `ShiftSegment`s. Overlap and worked time are computed segment by segment.
- **Double booking.** Two supervisors assigning the same person to the same shift at the same moment is stopped by a unique key, and returned as `409`.
- **Schema owned by Flyway.** Hibernate only validates it.

## Tech

Java 21 · Spring Boot 3 (Web, Data JPA, Security, OAuth2 Resource Server for JWT, Validation) · PostgreSQL 16 · Flyway · springdoc-openapi · JUnit 5, AssertJ, Testcontainers · Docker Compose · GitHub Actions

## Tests

```bash
mvn verify
```

Integration tests start a real PostgreSQL with Testcontainers, so Docker must be running.
