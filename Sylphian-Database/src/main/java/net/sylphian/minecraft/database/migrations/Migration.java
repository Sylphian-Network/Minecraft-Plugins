package net.sylphian.minecraft.database.migrations;

import org.jdbi.v3.core.Handle;

public interface Migration {
    int version();
    String description();
    void up(Handle handle);
}
