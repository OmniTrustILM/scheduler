package com.otilm.scheduler.config;

import java.util.Arrays;
import java.util.List;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Quartz tables exist only through Liquibase, which Spring Boot runs only from its own auto-configuration, so the
 * changelog has to run when the application starts. The database is this test's own, so what it finds applied, this
 * startup applied.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:liquibaseAtStartup;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;"
        + "INIT=CREATE SCHEMA IF NOT EXISTS SCHEDULER")
@ActiveProfiles("test")
class LiquibaseAtStartupTest {

    private JdbcTemplate jdbc;
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    void setJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Autowired
    void setBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Test
    void startupAppliesEveryChangelogInTheSchedulerSchema() {
        List<String> applied = jdbc
                .queryForList("select distinct filename from scheduler.databasechangelog order by filename",
                        String.class);

        assertEquals(List.of("db/migration/1.0.0/init-changelog.xml", "db/migration/1.1.1/rebrand-changelog.xml"),
                applied);
    }

    @Test
    void liquibaseWaitsForTheSchemaToBeCreated() {
        String[] liquibase = beanFactory.getBeanNamesForType(SpringLiquibase.class);
        assertEquals(1, liquibase.length, "Liquibase runs when the application starts");

        String[] dependsOn = beanFactory.getBeanDefinition(liquibase[0]).getDependsOn();
        String[] schemaInit = beanFactory.getBeanNamesForType(SchemaInit.SchemaInitBean.class);
        assertNotNull(dependsOn, "Liquibase depends on the bean that creates the schema");
        assertTrue(schemaInit.length > 0 && Arrays.asList(dependsOn).containsAll(Arrays.asList(schemaInit)),
                "Liquibase depends on the bean that creates the schema: " + Arrays.toString(dependsOn));
    }
}
