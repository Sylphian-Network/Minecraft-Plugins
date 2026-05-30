package net.sylphian.minecraft.database.migrations;

import org.jdbi.v3.core.Handle;

public interface Migration {
    int version();
    default String name() { return getClass().getSimpleName(); }
    String description();
    void up(Handle handle);
    default void down(Handle handle) {}
}