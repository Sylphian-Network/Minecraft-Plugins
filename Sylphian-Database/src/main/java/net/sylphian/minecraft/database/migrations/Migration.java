package net.sylphian.minecraft.database.migrations;

import org.jdbi.v3.core.Handle;

/**
 * Interface representing a single version-controlled database schema change.
 * Migrations are executed in ascending order of their version number.
 */
public interface Migration {

    /** @return the unique version number for this migration */
    int version();

    /** @return the name of the migration, defaults to the class name */
    default String name() { return getClass().getSimpleName(); }

    /** @return a brief description of what this migration achieves */
    String description();

    /**
     * Applies the migration changes to the database.
     *
     * @param handle the JDBI handle to use for executing SQL statements
     */
    void up(Handle handle);

    /**
     * Reverts the migration changes.
     * Implementations should undo exactly what was done in up().
     *
     * @param handle the JDBI handle to use for executing SQL statements
     */
    default void down(Handle handle) {}
}