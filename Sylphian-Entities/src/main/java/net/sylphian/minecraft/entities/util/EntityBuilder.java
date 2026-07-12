package net.sylphian.minecraft.entities.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.entities.entity.EntityStatKeys;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Utility class for spawning custom entities with a fluent API.
 * Custom stats are written to the entity's PersistentDataContainer at spawn
 * time (see {@link EntityStatKeys}) and mirrored onto the matching vanilla
 * attributes where one exists.
 *
 * <p>The terminal operation {@link #spawn(Location)} puts a live entity into
 * the world; each call spawns a fresh instance.</p>
 */
public class EntityBuilder {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final EntityType type;
    private Double health;
    private Double damage;
    private Double armor;
    private Component name;
    private boolean nameVisible = true;
    private boolean glowing = false;
    private boolean baby = false;
    private boolean persistent = false;
    private final Map<EquipmentSlot, EquipmentEntry> equipment = new EnumMap<>(EquipmentSlot.class);
    private final List<PotionEffect> potionEffects = new ArrayList<>();
    private final List<Consumer<PersistentDataContainer>> tags = new ArrayList<>();

    /**
     * Constructs a new EntityBuilder for the specified base entity type.
     *
     * @param type the base vanilla entity type, e.g. {@code EntityType.WOLF}
     */
    public EntityBuilder(EntityType type) {
        this.type = type;
    }

    /**
     * Sets the entity's max health. Applied to the {@code max_health} attribute
     * and written to the PersistentDataContainer; the entity spawns at full health.
     * Vanilla clamps the attribute to 1-1024; the PDC keeps the unclamped value.
     *
     * @param health the max health, must be positive
     * @return the builder instance
     */
    public EntityBuilder health(double health) {
        this.health = health;
        return this;
    }

    /**
     * Sets the entity's attack damage. Applied to the {@code attack_damage}
     * attribute and written to the PersistentDataContainer.
     * Vanilla clamps the attribute to 0-2048; the PDC keeps the unclamped value.
     *
     * @param damage the attack damage
     * @return the builder instance
     */
    public EntityBuilder damage(double damage) {
        this.damage = damage;
        return this;
    }

    /**
     * Sets the entity's armor value. Applied to the {@code armor} attribute
     * and written to the PersistentDataContainer.
     * Vanilla clamps the attribute to 0-30; the PDC keeps the unclamped value.
     *
     * @param armor the armor value
     * @return the builder instance
     */
    public EntityBuilder armor(double armor) {
        this.armor = armor;
        return this;
    }

    /**
     * Sets the entity's custom name using an Adventure Component.
     * The name is made visible and italic decoration is removed if not explicitly set.
     *
     * @param name the component name
     * @return the builder instance
     */
    public EntityBuilder name(Component name) {
        this.name = name.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        return this;
    }

    /**
     * Sets the entity's custom name using a MiniMessage string.
     *
     * @param miniMessage the MiniMessage name string
     * @return the builder instance
     */
    public EntityBuilder name(String miniMessage) {
        return name(MINI.deserialize(miniMessage));
    }

    /**
     * Sets whether the custom name is shown above the entity. Only has effect
     * when a name is set. Defaults to true.
     *
     * @param visible whether the name floats above the entity
     * @return the builder instance
     */
    public EntityBuilder nameVisible(boolean visible) {
        this.nameVisible = visible;
        return this;
    }

    /**
     * Sets whether the entity has the glowing outline.
     *
     * @param glowing whether the entity glows
     * @return the builder instance
     */
    public EntityBuilder glowing(boolean glowing) {
        this.glowing = glowing;
        return this;
    }

    /**
     * Sets whether the entity spawns as a baby. Applied to {@link Ageable}
     * mobs and to zombies; ignored for types that have no baby form.
     *
     * @param baby whether to spawn a baby
     * @return the builder instance
     */
    public EntityBuilder baby(boolean baby) {
        this.baby = baby;
        return this;
    }

    /**
     * Sets whether the entity persists indefinitely. When false, the entity
     * despawns naturally like a vanilla mob (removed when far from any player);
     * when true, it never despawns. Only affects {@link LivingEntity} types.
     * Defaults to false.
     *
     * @param persistent whether the entity never despawns
     * @return the builder instance
     */
    public EntityBuilder persistent(boolean persistent) {
        this.persistent = persistent;
        return this;
    }

    /**
     * Adds a potion effect applied at spawn time with hidden particles and icon.
     *
     * @param type          the effect type
     * @param amplifier     the effect amplifier, 0 being level I
     * @param durationTicks the duration in ticks, or {@link PotionEffect#INFINITE_DURATION} for infinite
     * @return the builder instance
     */
    public EntityBuilder potionEffect(PotionEffectType type, int amplifier, int durationTicks) {
        potionEffects.add(new PotionEffect(type, durationTicks, amplifier, false, false, false));
        return this;
    }

    /**
     * Equips the entity with an item in the given slot, leaving the vanilla drop
     * chance for that slot. Pull the ItemStack from {@code ItemRegistry} for
     * namespaced custom gear.
     *
     * @param slot the equipment slot
     * @param item the item to equip
     * @return the builder instance
     */
    public EntityBuilder equipment(EquipmentSlot slot, ItemStack item) {
        return equipment(slot, item, null);
    }

    /**
     * Equips the entity with an item in the given slot and sets its drop chance.
     *
     * @param slot       the equipment slot
     * @param item       the item to equip
     * @param dropChance the chance (0.0-1.0) the item drops on death, or null to
     *                   leave the vanilla default for the slot
     * @return the builder instance
     */
    public EntityBuilder equipment(EquipmentSlot slot, ItemStack item, @Nullable Float dropChance) {
        equipment.put(slot, new EquipmentEntry(item, dropChance));
        return this;
    }

    /**
     * Writes a custom double value into the entity's PersistentDataContainer at spawn time.
     *
     * @param key   the tag key
     * @param value the value to store
     * @return the builder instance
     */
    public EntityBuilder tag(NamespacedKey key, double value) {
        tags.add(pdc -> pdc.set(key, PersistentDataType.DOUBLE, value));
        return this;
    }

    /**
     * Writes a custom int value into the entity's PersistentDataContainer at spawn time.
     *
     * @param key   the tag key
     * @param value the value to store
     * @return the builder instance
     */
    public EntityBuilder tag(NamespacedKey key, int value) {
        tags.add(pdc -> pdc.set(key, PersistentDataType.INTEGER, value));
        return this;
    }

    /**
     * Writes a custom String value into the entity's PersistentDataContainer at spawn time.
     *
     * @param key   the tag key
     * @param value the value to store
     * @return the builder instance
     */
    public EntityBuilder tag(NamespacedKey key, String value) {
        tags.add(pdc -> pdc.set(key, PersistentDataType.STRING, value));
        return this;
    }

    /**
     * Spawns the entity into the world with all configured stats, name,
     * equipment, and tags applied. Must be called on the main thread.
     *
     * @param location the location to spawn at
     * @return the live spawned entity
     */
    public Entity spawn(Location location) {
        World world = location.getWorld();
        Entity entity = world.spawnEntity(location, type, CreatureSpawnEvent.SpawnReason.CUSTOM, this::apply);
        // Applied after spawn: an age set inside the pre-spawn function can be
        // overwritten when the entity finalizes (e.g. the vanilla baby roll).
        applyAge(entity);
        return entity;
    }

    private void apply(Entity entity) {
        if (name != null) {
            entity.customName(name);
            entity.setCustomNameVisible(nameVisible);
        }
        if (glowing) entity.setGlowing(true);

        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (health != null) pdc.set(EntityStatKeys.HEALTH, PersistentDataType.DOUBLE, health);
        if (damage != null) pdc.set(EntityStatKeys.DAMAGE, PersistentDataType.DOUBLE, damage);
        if (armor != null) pdc.set(EntityStatKeys.ARMOR, PersistentDataType.DOUBLE, armor);
        tags.forEach(tag -> tag.accept(pdc));

        if (entity instanceof LivingEntity living) {
            living.setPersistent(persistent);
            living.setRemoveWhenFarAway(!persistent);

            if (health != null) {
                double effectiveMax = applyAttribute(living, Attribute.MAX_HEALTH, health);
                living.setHealth(Math.min(health, effectiveMax));
            }
            if (damage != null) applyAttribute(living, Attribute.ATTACK_DAMAGE, damage);
            if (armor != null) applyAttribute(living, Attribute.ARMOR, armor);

            if (!equipment.isEmpty()) {
                EntityEquipment entityEquipment = living.getEquipment();
                if (entityEquipment != null) {
                    equipment.forEach((slot, entry) -> {
                        entityEquipment.setItem(slot, entry.item());
                        if (entry.dropChance() != null) entityEquipment.setDropChance(slot, entry.dropChance());
                    });
                }
            }

            potionEffects.forEach(living::addPotionEffect);
        }
    }

    private void applyAge(Entity entity) {
        if (entity instanceof Ageable ageable) {
            if (baby) ageable.setBaby();
            else ageable.setAdult();
        }
    }

    /**
     * Sets the base value of a vanilla attribute, registering it on the entity
     * first if it is missing.
     *
     * @return the effective value after vanilla clamping, or the requested value if the attribute could not be applied
     */
    private double applyAttribute(LivingEntity living, Attribute attribute, double value) {
        AttributeInstance instance = living.getAttribute(attribute);
        if (instance == null) {
            living.registerAttribute(attribute);
            instance = living.getAttribute(attribute);
        }
        if (instance == null) return value;

        instance.setBaseValue(value);
        return instance.getValue();
    }
}
