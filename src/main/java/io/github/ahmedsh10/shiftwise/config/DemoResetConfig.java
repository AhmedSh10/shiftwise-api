package io.github.ahmedsh10.shiftwise.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Public demo only: visitors can change anything, so the database is rebuilt
 * from the Flyway migrations (schema + demo data) on every start and once a night.
 * Never enable the "demo" profile against real data.
 */
@Configuration
@Profile("demo")
@EnableScheduling
public class DemoResetConfig {

    private static final Logger log = LoggerFactory.getLogger(DemoResetConfig.class);

    private final ObjectProvider<Flyway> flyway;

    public DemoResetConfig(ObjectProvider<Flyway> flyway) {
        this.flyway = flyway;
    }

    @Bean
    public static FlywayMigrationStrategy cleanThenMigrate() {
        return f -> reset(f, "startup");
    }

    @Scheduled(cron = "${shiftwise.demo.reset-cron:0 0 3 * * *}", zone = "Europe/Rome")
    public void nightlyReset() {
        reset(flyway.getObject(), "nightly");
    }

    private static synchronized void reset(Flyway f, String reason) {
        log.warn("Demo profile: resetting database ({})", reason);
        f.clean();
        f.migrate();
    }
}
