package io.github.ahmedsh10.shiftwise.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/** The live-demo profile must put the database back to the seeded state. Own container: it wipes data. */
@SpringBootTest
@ActiveProfiles("demo")
@Testcontainers
class DemoResetIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    DemoResetConfig demoReset;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void nightlyResetRemovesVisitorChangesAndRestoresDemoData() {
        Integer seededEmployees = jdbc.queryForObject("SELECT count(*) FROM employees", Integer.class);
        jdbc.update("DELETE FROM employee_areas WHERE employee_id = (SELECT max(id) FROM employees)");
        jdbc.update("INSERT INTO areas (site_id, code, name) VALUES (1, 'VISITOR', 'Added by a visitor')");
        jdbc.update("UPDATE areas SET name = 'Renamed' WHERE code = 'WARDS'");

        demoReset.nightlyReset();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM areas WHERE code = 'VISITOR'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT name FROM areas WHERE code = 'WARDS'", String.class))
                .isEqualTo("Hospital wards");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM employees", Integer.class)).isEqualTo(seededEmployees);
    }
}
