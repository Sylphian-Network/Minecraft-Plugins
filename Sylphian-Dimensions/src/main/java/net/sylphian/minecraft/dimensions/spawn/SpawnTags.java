package net.sylphian.minecraft.dimensions.spawn;

import org.bukkit.NamespacedKey;

/**
 * PersistentDataContainer keys written onto entities produced by the dimension
 * spawner, used to count them against the per-dimension caps.
 */
public final class SpawnTags {

    /** Marks a spawner-produced entity; the value is the source dimension name. */
    public static final NamespacedKey SOURCE = new NamespacedKey("sylphian-dimensions", "spawn-source");

    private SpawnTags() {}
}
