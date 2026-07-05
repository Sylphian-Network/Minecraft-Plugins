package net.sylphian.minecraft.database.migrations;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MigrationRunnerTest {

    private static final Logger LOGGER = Logger.getLogger(MigrationRunnerTest.class.getName());
    private static final String PLUGIN = "Test-Plugin";

    private Jdbi jdbi;

    @BeforeEach
    void setUp() {
        jdbi = Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    }

    private void apply(MigrationRunner runner) {
        jdbi.useHandle(handle -> runner.applyPending(handle, LOGGER));
    }

    private List<Integer> recordedVersions() {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT version FROM schema_migrations WHERE plugin = :plugin ORDER BY version")
                        .bind("plugin", PLUGIN)
                        .mapTo(Integer.class)
                        .list());
    }

    @Test
    void migrationsApplyInVersionOrderRegardlessOfInputOrder() {
        List<Integer> order = new ArrayList<>();
        MigrationRunner runner = new MigrationRunner(jdbi,
                List.of(new RecordingMigration(2, order), new RecordingMigration(1, order), new RecordingMigration(3, order)),
                PLUGIN);

        apply(runner);

        assertThat(order).containsExactly(1, 2, 3);
        assertThat(recordedVersions()).containsExactly(1, 2, 3);
    }

    @Test
    void secondRunAppliesNothing() {
        List<Integer> order = new ArrayList<>();
        MigrationRunner runner = new MigrationRunner(jdbi, List.of(new RecordingMigration(1, order)), PLUGIN);

        apply(runner);
        apply(runner);

        assertThat(order).containsExactly(1);
        assertThat(recordedVersions()).containsExactly(1);
    }

    @Test
    void failedMigrationRunsItsDownAndRecordsNothing() {
        List<Integer> order = new ArrayList<>();
        List<String> events = new ArrayList<>();
        Migration failing = new FailingMigration(2, events);
        MigrationRunner runner = new MigrationRunner(jdbi, List.of(new RecordingMigration(1, order), failing), PLUGIN);

        assertThatThrownBy(() -> apply(runner))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("V2");

        assertThat(events).containsExactly("up", "down");
        // V1 succeeded before the failure and stays recorded; V2 must not be recorded.
        assertThat(recordedVersions()).containsExactly(1);
    }

    @Test
    void trackingRowRecordsPluginVersionAndName() {
        MigrationRunner runner = new MigrationRunner(jdbi, List.of(new RecordingMigration(1, new ArrayList<>())), PLUGIN);

        apply(runner);

        Map<String, Object> row = jdbi.withHandle(handle ->
                handle.createQuery("SELECT plugin, version, name, description FROM schema_migrations")
                        .mapToMap()
                        .one());
        assertThat(row.get("plugin")).isEqualTo(PLUGIN);
        assertThat(row.get("version")).isEqualTo(1);
        assertThat(row.get("name")).isEqualTo("RecordingMigration");
        assertThat(row.get("description")).isEqualTo("test migration 1");
    }

    /** Creates no schema; records the order its up() was invoked in. */
    private static final class RecordingMigration implements Migration {
        private final int version;
        private final List<Integer> order;

        RecordingMigration(int version, List<Integer> order) {
            this.version = version;
            this.order = order;
        }

        @Override public int version() { return version; }
        @Override public String description() { return "test migration " + version; }
        @Override public void up(Handle handle) { order.add(version); }
    }

    /** Throws from up(); records up/down invocations. */
    private static final class FailingMigration implements Migration {
        private final int version;
        private final List<String> events;

        FailingMigration(int version, List<String> events) {
            this.version = version;
            this.events = events;
        }

        @Override public int version() { return version; }
        @Override public String description() { return "always fails"; }

        @Override
        public void up(Handle handle) {
            events.add("up");
            throw new IllegalStateException("boom");
        }

        @Override
        public void down(Handle handle) {
            events.add("down");
        }
    }
}
