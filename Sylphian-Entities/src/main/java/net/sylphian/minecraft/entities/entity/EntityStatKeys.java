package net.sylphian.minecraft.entities.entity;

import org.bukkit.NamespacedKey;

/**
 * PersistentDataContainer keys for custom entity stats.
 *
 * <p>Written at spawn time by {@code EntityBuilder} and read by combat-handling
 * plugins during damage events. All values are stored as {@code DOUBLE}.</p>
 */
public final class EntityStatKeys {

    /** Custom max health written at spawn time. */
    public static final NamespacedKey HEALTH = new NamespacedKey("sylphian-entities", "health");

    /** Custom attack damage written at spawn time. */
    public static final NamespacedKey DAMAGE = new NamespacedKey("sylphian-entities", "damage");

    /** Custom armor value written at spawn time. */
    public static final NamespacedKey ARMOR = new NamespacedKey("sylphian-entities", "armor");

    private EntityStatKeys() {}
}
