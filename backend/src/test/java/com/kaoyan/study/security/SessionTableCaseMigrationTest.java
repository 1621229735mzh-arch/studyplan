package com.kaoyan.study.security;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.mysql.MySQLContainer;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionTableCaseMigrationTest {
    private static final String MYSQL_IMAGE =
            "mysql@sha256:0744ee5ef89ce6ccfa13de3e579fe6b9e27f93dd70da9c06d2c908b1b193fb8d";

    @Test
    void repairsWindowsExportOnCaseSensitiveLinuxWithoutLosingRows() {
        verifyMigration(0);
    }

    @Test
    void remainsCompatibleWithCaseInsensitiveWindowsStyleSchema() {
        verifyMigration(1);
    }

    private void verifyMigration(int lowerCaseTableNames) {
        try (var mysql = new MySQLContainer(MYSQL_IMAGE)
                .withCommand("--lower-case-table-names=" + lowerCaseTableNames)) {
            mysql.start();
            var dataSource = new DriverManagerDataSource(
                    mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
            var jdbc = new JdbcTemplate(dataSource);
            Flyway.configure().dataSource(dataSource).target("12").load().migrate();
            var repository = new JdbcIndexedSessionRepository(jdbc,
                    new TransactionTemplate(new JdbcTransactionManager(dataSource)));
            verifySessions(dataSource, jdbc, repository, lowerCaseTableNames);
            repository.cleanUpExpiredSessions();
        }
    }

    private <S extends Session> void verifySessions(DataSource dataSource, JdbcTemplate jdbc,
            SessionRepository<S> repository, int lowerCaseTableNames) {
        var session = repository.createSession();
        session.setAttribute("case-test", "preserved");
        repository.save(session);
        long tasksBefore = jdbc.queryForObject("SELECT COUNT(*) FROM task", Long.class);

        if (lowerCaseTableNames == 0) {
            // Model the names emitted by a Windows dump imported on Linux.
            jdbc.execute("RENAME TABLE SPRING_SESSION TO spring_session, "
                    + "SPRING_SESSION_ATTRIBUTES TO spring_session_attributes");
            assertThatThrownBy(() -> repository.findById(session.getId()))
                    .isInstanceOf(org.springframework.jdbc.BadSqlGrammarException.class);
        }

        var flyway = Flyway.configure().dataSource(dataSource).target("13").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM task", Long.class))
                .isEqualTo(tasksBefore);
        var restored = repository.findById(session.getId());
        assertThat(restored).isNotNull();
        assertThat((String) restored.getAttribute("case-test")).isEqualTo("preserved");
        restored.setAttribute("case-test", "updated");
        repository.save(restored);
        assertThat((String) repository.findById(session.getId()).getAttribute("case-test"))
                .isEqualTo("updated");
        var newSession = repository.createSession();
        newSession.setAttribute("case-test", "new");
        repository.save(newSession);
        assertThat(repository.findById(newSession.getId())).isNotNull();
        repository.deleteById(session.getId());
        repository.deleteById(newSession.getId());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION_ATTRIBUTES", Long.class))
                .isZero();
    }
}
